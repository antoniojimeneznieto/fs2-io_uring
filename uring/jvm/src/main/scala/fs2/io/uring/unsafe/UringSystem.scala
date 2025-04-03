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

import io.netty.incubator.channel.uring.UringRing
import io.netty.incubator.channel.uring.UringSubmissionQueue
import io.netty.incubator.channel.uring.UringCompletionQueue
import io.netty.incubator.channel.uring.UringCompletionQueueCallback
import io.netty.incubator.channel.uring.NativeAccess
import io.netty.incubator.channel.uring.Encoder

import fs2.io.uring.unsafe.util.OP._

import scala.collection.mutable.Map

import java.nio.ByteBuffer

import io.netty.channel.unix.FileDescriptor

import java.util.BitSet
import java.util.concurrent.ConcurrentLinkedDeque
import java.nio.channels.spi.AbstractSelector
import java.{util => ju}
import java.nio.channels.Selector
import java.nio.channels.SelectionKey
import java.nio.channels.spi.AbstractSelectableChannel
import cats.effect.unsafe.PollingContext
import cats.effect.unsafe.PollResult
import cats.effect.unsafe.metrics.PollerMetrics

object UringSystem extends PollingSystem {

  type Api = Uring

  type Poller = PollerImpl

  override def close(): Unit = ???

  override def makeApi(ctx: PollingContext[Poller]): Api = ???

  override def makePoller(): Poller = ???

  override def closePoller(poller: Poller): Unit = ???

  override def poll(poller: Poller, nanos: Long): PollResult = ???

  override def processReadyEvents(poller: Poller): Boolean = ???

  override def needsPoll(poller: Poller): Boolean = ???

  override def interrupt(targetThread: Thread, targetPoller: Poller): Unit = ???

  override def metrics(poller: Poller): PollerMetrics = ???

  private final class ApiImpl(ctx: PollingContext[Poller]) extends Uring {

    override def call(
        op: Byte,
        flags: Int,
        rwFlags: Int,
        fd: Int,
        bufferAddress: Long,
        length: Int,
        offset: Long,
        mask: Int => Boolean
    ): IO[Int] = ???

    override def bracket(
        op: Byte,
        flags: Int,
        rwFlags: Int,
        fd: Int,
        bufferAddress: Long,
        length: Int,
        offset: Long,
        mask: Int => Boolean
    )(release: Int => IO[Unit]): Resource[IO, Int] = ???

  }

  final class PollerImpl(ring: UringRing) extends AbstractSelector(null) {

    //////////////////////////////////////////////////////////////////////
    // Due to AbstractSelector, we need to implement all of these methods
    //////////////////////////////////////////////////////////////////////
    override def keys(): ju.Set[SelectionKey] = ???

    override def selectedKeys(): ju.Set[SelectionKey] = ???

    override def selectNow(): Int = ???

    override def select(timeout: Long): Int = ???

    override def select(): Int = ???

    override def wakeup(): Selector = ???

    override protected def implCloseSelector(): Unit = ???

    override protected def register(
        ch: AbstractSelectableChannel,
        ops: Int,
        att: Object
    ): SelectionKey = ???

  }

}
