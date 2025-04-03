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
import cats.effect.FileDescriptorPoller
import cats.effect.FileDescriptorPollHandle
import cats.effect.IO
import cats.effect.kernel.Cont
import cats.effect.kernel.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.effect.std.Mutex
import cats.effect.unsafe.{PollingSystem, PollingContext, PollResult}
import cats.effect.unsafe.PollingContext
import cats.effect.unsafe.metrics.PollerMetrics
import cats.syntax.all._

import java.util.Collections
import java.util.IdentityHashMap
import java.util.Set
import scala.scalanative.posix.errno._
import scala.scalanative.posix.pollEvents._
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

import uring._
import uringOps._

object UringSystem extends PollingSystem {

  private final val MaxEvents = 64

  type Api = Uring with FileDescriptorPoller
  def close(): Unit = ???

  def makeApi(ctx: PollingContext[Poller]): Api = ???

  def makePoller(): Poller = ???

  def closePoller(poller: Poller): Unit = ???

  def poll(poller: Poller, nanos: Long): PollResult = ???

  def processReadyEvents(poller: Poller): Boolean = ???

  def needsPoll(poller: Poller): Boolean = ???

  def interrupt(targetThread: Thread, targetPoller: Poller): Unit = ???

  def metrics(poller: Poller): PollerMetrics = ???

  private final class ApiImpl private[UringSystem] (ctx: PollingContext[Poller])
      extends Uring
      with FileDescriptorPoller {

    def call(prep: Ptr[io_uring_sqe] => Unit, mask: Int => Boolean): IO[Int] = ???

    def bracket(prep: Ptr[io_uring_sqe] => Unit, mask: Int => Boolean)(
        release: Int => IO[Unit]
    ): Resource[IO, Int] = ???
  }

  final class Poller private[UringSystem] (ring: Ptr[io_uring]) {}

}
