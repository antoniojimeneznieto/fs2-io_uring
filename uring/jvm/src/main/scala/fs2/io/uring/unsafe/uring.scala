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

package fs2.io.uring.unsafe

import java.lang.foreign.MemorySegment;
import cheshire._;
import scala.collection.mutable.HashMap

private[uring] object uringOps {
  private val cache: HashMap[Long, AnyRef] = HashMap()

  def io_uring_sqe_set_data[A <: AnyRef](sqe: MemorySegment, data: A): Unit = {
    val hashCode: Long = System.identityHashCode(data).toLong
    cache.addOne((hashCode, data))
    io_uring_sqe.setUserData(sqe, hashCode)
  }

  def io_uring_cqe_get_data[A <: AnyRef](cqe: MemorySegment): A = {
    val hashCode = io_uring_sqe.getUserData(cqe)
    val res: A = cache.get(hashCode).asInstanceOf[A]
    cache.remove(hashCode)
    res
  }

}
