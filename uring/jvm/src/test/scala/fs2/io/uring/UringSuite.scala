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

import munit.CatsEffectSuite
import cats.effect.unsafe.IORuntime
import fs2.io.uring.unsafe.UringSystem
import cats.effect.unsafe.IORuntimeBuilder
import scala.concurrent.duration._


abstract class UringSuite extends CatsEffectSuite {

  override lazy val munitIORuntime = {
    val (pool, poller, shutdown) =
      IORuntime.createWorkStealingComputeThreadPool(threads = 2, pollingSystem = UringSystem)
    IORuntime.builder().setCompute(pool, shutdown).addPoller(poller, () => ()).build()
  }

  // override lazy val munitIORuntime =
  //   IORuntimeBuilder()
  //     .setPollingSystem(UringSystem)
  //     .build()

  override def munitIOTimeout: Duration = 3.second
}
