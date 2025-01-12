/*
 * Copyright 2022 Arman Bilge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fs2.io.uring
package unsafe

import cats.~>
import cats.syntax.all._

import cats.effect.IO

import cats.effect.kernel.Resource
import cats.effect.kernel.MonadCancelThrow
import cats.effect.kernel.Cont

import cats.effect.unsafe.PollingSystem

import scala.collection.mutable.Map

import java.util.concurrent.ConcurrentLinkedDeque
import java.nio.channels.spi.AbstractSelector
import java.{util => ju}
import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.nio.channels.spi.AbstractSelectableChannel

import fs2.io.uring.unsafe.util._

import cheshire._
import cheshire.liburing._
import cheshire.constants._

import uringOps._

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment


object UringSystem extends PollingSystem {

  private val debug = true
  private val debugPoll = debug && true
  private val debugCancel = debug && false
  private val debugInterrupt = debug && false
  // private val debugSubmissionQueue = debug && false
  // private val debugHandleCompletionQueue = debug && true

  private final val MaxEvents = 64
  private val session: Arena = Arena.ofShared()

  type Api = Uring

  override def makeApi(access: (Poller => Unit) => Unit): Api = new ApiImpl(access)

  override def makePoller(): Poller = {
    val ring = new io_uring(session)

    val flags = IORING_SETUP_SUBMIT_ALL |
      IORING_SETUP_COOP_TASKRUN |
      IORING_SETUP_TASKRUN_FLAG |
      IORING_SETUP_SINGLE_ISSUER |
      IORING_SETUP_DEFER_TASKRUN

    // the submission queue size need not exceed 64
    // every submission is accompanied by async suspension,
    // and at most 64 suspensions can happen per iteration
    val e = io_uring_queue_init(64, ring, flags)
    if (e < 0) throw IOExceptionHelper(-e)

    new Poller(ring)
  }

  override def close(): Unit = ()

  override def closePoller(poller: Poller): Unit = poller.close()

  override def poll(
      poller: Poller,
      nanos: Long,
      reportFailure: Throwable => Unit
  ): Boolean =
    poller.poll(nanos)

  override def needsPoll(poller: Poller): Boolean = poller.needsPoll()

  def getCurrentPollerIfAvailable(): Option[Poller] = None // TODO

  override def interrupt(targetThread: Thread, targetPoller: Poller): Unit = {
    if (debugInterrupt)
      println(
        s"[INTERRUPT ${Thread.currentThread().getName()}] waking up poller: ${targetPoller.getFd()} in thread: $targetThread"
      )
    getCurrentPollerIfAvailable() match {
      case Some(poller) => poller.sendMsg(targetPoller.getFd())
      case None         => targetPoller.wakeup()
    }
    ()
  }

  private final class ApiImpl(access: (Poller => Unit) => Unit) extends Uring {
    private[this] val noopRelease: Int => IO[Unit] = _ => IO.unit

    def call(prep: io_uring_sqe => Unit, mask: Int => Boolean): IO[Int] =
      exec(prep, mask)(noopRelease)

    def bracket(prep: io_uring_sqe => Unit, mask: Int => Boolean)(
        release: Int => IO[Unit]
    ): Resource[IO, Int] =
      Resource.makeFull[IO, Int](poll => poll(exec(prep, mask)(release(_))))(release(_))

    private def exec(prep: io_uring_sqe => Unit, mask: Int => Boolean)(
        release: Int => IO[Unit]
    ): IO[Int] = {

      def cancel(
          operationAddress: Long,
          correctRing: Poller
      ): IO[Boolean] =
        IO.async_[Int] { cb =>
          access { ring =>
            if (debugCancel)
              println(
                s"[CANCEL ring:${ring.getFd()}] cancel an operation with address: $operationAddress"
              )
            if (correctRing == ring) {
              if (debugCancel)
                println(
                  s"[CANCEL ring:${ring.getFd()}] Cancelling from the same ring!"
                )
              val sqe = ring.getSqe(cb)
              io_uring_prep_cancel64(sqe, operationAddress, 0)
            } else {
              if (debugCancel)
                println(
                  s"[CANCEL ring:${ring.getFd()}] Cancelling from another ring: cancelled operation is in: ${correctRing.getFd()}"
                )
              correctRing.enqueueCancelOperation(operationAddress, cb)
            }
            ()
          }
        }.map(_ == 0)

      IO.cont {
        new Cont[IO, Int, Int] {
          def apply[F[_]](implicit
              F: MonadCancelThrow[F]
          ): (Either[Throwable, Int] => Unit, F[Int], IO ~> F) => F[Int] = { (resume, get, lift) =>
            F.uncancelable { poll =>
              val submit: IO[(Long, Poller)] = IO.async_[(Long, Poller)] { cb =>
                access { ring =>
                  val sqe = ring.getSqe(resume)
                  prep(sqe)
                  val userData =
                    io_uring_sqe.getUserData(sqe.segment)
                  cb(Right((userData, ring)))
                }
              }

              lift(submit)
                .flatMap { case (addr, ring) =>
                  F.onCancel(
                    poll(get),
                    lift(cancel(addr, ring)).ifM(
                      F.unit,
                      // if cannot cancel, fallback to get
                      get.flatMap { rtn =>
                        if (rtn < 0 && !mask(rtn)) F.raiseError(IOExceptionHelper(-rtn))
                        else lift(release(rtn))
                      }
                    )
                  )
                }
                .flatTap(e => F.raiseWhen(e < 0 && !mask(-e))(IOExceptionHelper(-e)))
            }
          }
        }
      }
    }
  }

  final class Poller private[UringSystem] (ring: io_uring) extends AbstractSelector(null) {

    // private[this] var listenFd: Boolean = false

    private[this] val interruptRing: io_uring = {
      val interrRing = new io_uring(session)
      val e = io_uring_queue_init(64, interrRing, 0)
      if (e < 0) throw IOExceptionHelper(-e)
      interrRing
    }

    private[this] val cancelOperations
        : ConcurrentLinkedDeque[(Long, Either[Throwable, Int] => Unit)] =
      new ConcurrentLinkedDeque()

    private[this] var pendingSubmissions: Boolean = false
    private[this] val callbacks: Map[Long, Either[Throwable, Int] => Unit] =
      Map.empty[Long, Either[Throwable, Int] => Unit]

    // API

    private[UringSystem] def getSqe(cb: Either[Throwable, Int] => Unit): io_uring_sqe = {
      pendingSubmissions = true
      val sqeSegment = io_uring_get_sqe(ring)
      val sqe = new io_uring_sqe(sqeSegment)
      // TODO: Ask (castObjectToRawPtr(data)).toULong
      io_uring_sqe_set_data(sqeSegment, cb)
      val userData = io_uring_sqe.getUserData(sqeSegment)
      callbacks.put(userData, cb)
      sqe
    }

    private[UringSystem] def getFd(): Int = io_uring.getRingFd(ring.segment)

    private[UringSystem] def needsPoll(): Boolean = pendingSubmissions || !callbacks.isEmpty

    private[UringSystem] def enqueueCancelOperation(
        operationAddress: Long,
        cb: Either[Throwable, Int] => Unit
    ): Unit = {
      cancelOperations.add((operationAddress, cb))
      wakeup()
      ()
    }

    private[UringSystem] def sendMsg(fd: Int): Unit = {
      val sqeSegment = io_uring_get_sqe(ring)
      io_uring_sqe.setOpcode(sqeSegment, OP.IORING_OP_MSG_RING)
      io_uring_sqe.setFd(sqeSegment, fd)
    }

    private[UringSystem] def poll(
        nanos: Long
    ): Boolean =
      try {
        begin()

        if (debugPoll)
          println(s"[POLL ${Thread.currentThread().getName()}] Polling with nanos = $nanos")

        // startListening() // Check if it is listening to the FD. If not, start listening

        checkCancelOperations() // Check for cancel operations
        
        val rtn = if (nanos == 0) {
          if (pendingSubmissions) io_uring_submit(ring)
          else 0
        } else {
          val timeoutSpec =
            if (nanos == -1) {
              new __kernel_timespec(MemorySegment.NULL)
            } else {
              val ts = new __kernel_timespec(session)
              __kernel_timespec.setTvSec(ts.segment, nanos / 1000000000);
              __kernel_timespec.setTvNsec(ts.segment, nanos % 1000000000);
              ts
            }
          val cqe = new io_uring_cqe(session)
          if (pendingSubmissions) {
            io_uring_submit_and_wait_timeout(ring, cqe, 0, timeoutSpec, MemorySegment.NULL)
          } else {
            io_uring_wait_cqe_timeout(ring, cqe, timeoutSpec)
          }
        }

        processPendingSubmissions(ring, rtn)
      } finally
        end()

    // private

    // CALLBACKS

    private[this] def processPendingSubmissions(ring: io_uring, _rtn: Int): Boolean = {
      var rtn = _rtn
      val cqes = new io_uring_cqes(session, MaxEvents)
      val invokedCbs = processCqes(cqes, ring)

      if (pendingSubmissions && rtn == -ERR.EBUSY) {
        // submission failed, so try again
        rtn = io_uring_submit(ring)
        while (rtn == -ERR.EBUSY) {
          processCqes(cqes, ring)
          rtn = io_uring_submit(ring)
        }
      }

      pendingSubmissions = false
      invokedCbs
    }

    private[this] def processCqes(cqes: io_uring_cqes, ring: io_uring): Boolean = {
      val filledCount = io_uring_peek_batch_cqe(ring, cqes.segment, MaxEvents)

      var i = 0
      while (i < filledCount) {
        val cqeSegment =
          io_uring_cqes.getCqeAtIndex(cqes.segment, i)
        val cb = io_uring_cqe_get_data[Either[Exception, Int] => Unit](cqeSegment)
        val res = io_uring_cqe.getRes(cqeSegment)
        val userData = io_uring_cqe.getUserData(cqeSegment)
        cb(Right(res))
        callbacks.remove(userData)

        i += 1
      }

      io_uring_cq_advance(ring, filledCount)
      filledCount > 0
    }

    // POLL

    // private[this] def startListening(): Unit =
    //   if (!listenFd) {
    //     if (debugPoll)
    //       println(s"[POLL ${Thread.currentThread().getName()}] We are not listening to the FD!")

    //     val sqeSegment = io_uring_get_sqe(ring)
    //     val sqe = new io_uring_sqe() // new io_uring_sqe(sqeSegment)
    //     sqe.setOpcode(
    //       sqeSegment,
    //       OP.IORING_OP_POLL_ADD
    //     ) // io_uring_sqe.setOpcode(sqe, IORING_OP_POLL_ADD)
    //     sqe.setFlags(sqeSegment, 0) // io_uring_sqe.setFlags(sqe, 0)
    //     sqe.setRwFlags(sqeSegment, POLLIN) // io_uring_sqe.setRwFlags(sqe, POLLIN)
    //     sqe.setFd(sqeSegment, readEnd) // io_uring_sqe.setFd(sqe, readEnd)
    //     sqe.setOff(sqeSegment, 0) // io_uring_sqe.setOff(sqe, 0)
    //     sqe.setAddr(sqeSegment, 0) // io_uring_sqe.setAddr(sqe, 0)
    //     sqe.setUserData(sqeSegment, POLLIN) // io_uring_sqe.setUserData(sqe, POLLIN)

    //     pendingSubmissions = true
    //     listenFd = true // Set the flag indicating it is now listening
    //   }

    private[this] def checkCancelOperations(): Unit =
      if (!cancelOperations.isEmpty()) {
        if (debugPoll)
          println(
            s"[POLL ${Thread.currentThread().getName()}] The Cancel Queue is not empty, it has: ${cancelOperations.size()} elements"
          )
        cancelOperations.forEach { case (operationAddress, cb) =>
          val sqe = getSqe(cb) // ring.getSqe(cb)
          io_uring_prep_cancel64(sqe, operationAddress, 0)
        }
        cancelOperations.clear()
      }

    // ABSTRACT SELECTOR

    override def keys(): ju.Set[SelectionKey] = throw new UnsupportedOperationException

    override def selectedKeys(): ju.Set[SelectionKey] = throw new UnsupportedOperationException

    override def selectNow(): Int = throw new UnsupportedOperationException

    override def select(x$1: Long): Int = throw new UnsupportedOperationException

    override def select(): Int = throw new UnsupportedOperationException

    override def wakeup(): Selector = {
      processPendingSubmissions(interruptRing, 0)

      // uringSubmissionQueue.enqueueSqe(40, flags, 0, fd, 0, 0, 0, 0)
      // uringSubmissionQueue.submitAndWait()

      // TODO: needs getSqe(cb) logic?

      val sqeSegment = io_uring_get_sqe(interruptRing)
      if(sqeSegment != MemorySegment.NULL){
        io_uring_sqe.setOpcode(sqeSegment, OP.IORING_OP_MSG_RING)
        io_uring_sqe.setFlags(sqeSegment, 0)
        io_uring_sqe.setRwFlags(sqeSegment, 0)
        io_uring_sqe.setFd(sqeSegment, this.getFd())
        io_uring_sqe.setOff(sqeSegment, 0)
        io_uring_sqe.setAddr(sqeSegment, 0)
        io_uring_sqe.setUserData(sqeSegment, 0)
      }

      io_uring_submit_and_wait(interruptRing, 0) // TODO: Should be 0?

      this
    }

    override protected def implCloseSelector(): Unit =
      // readEnd.close()
      // writeEnd.close()
      io_uring_queue_exit(ring)
    // util.free(ring) // TODO

    override protected def register(
        x$1: AbstractSelectableChannel,
        x$2: Int,
        x$3: Object
    ): SelectionKey = throw new UnsupportedOperationException

  }
}
