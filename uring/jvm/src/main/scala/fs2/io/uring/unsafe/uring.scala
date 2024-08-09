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

private[uring] object uring {
  final val IORING_SETUP_SUBMIT_ALL = 1 << 7
  final val IORING_SETUP_COOP_TASKRUN = 1 << 8
  final val IORING_SETUP_TASKRUN_FLAG = 1 << 9
  final val IORING_SETUP_SINGLE_ISSUER = 1 << 12
  final val IORING_SETUP_DEFER_TASKRUN = 1 << 13

  def io_uring_peek_cqe(ring: io_uring, cqePtr: io_uring_cqe, nrAvailable: MemorySegment) = ???
  def io_uring_peek_cqe(ring: io_uring, cqePtr: io_uring_cqe) = ???
  def io_uring_wait_cqe(ring: io_uring, cqePtr: io_uring_cqe) = ???
  def io_uring_cqe_seen(ring: io_uring, cqePtr: io_uring_cqe) = ???
  def io_uring_queue_init(entries: Int, ring: io_uring, flags: Int): Int = ???
  def io_uring_queue_exit(ring: io_uring): Unit = ???
  def io_uring_get_sqe(ring: io_uring): MemorySegment = ???
  def io_uring_submit(ring: io_uring): Int = ???
  def io_uring_submit_and_wait(ring: io_uring, waitNr: Int): Int = ???
  def io_uring_submit_and_wait_timeout(
      ring: io_uring,
      cqePtr: io_uring_cqe,
      waitNr: Int,
      ts: __kernel_timespec,
      sigmask: MemorySegment
  ) = ???
  def io_uring_wait_cqe_timeout(ring: io_uring, cqePtr: io_uring_cqe, ts: __kernel_timespec) = ???
  def io_uring_peek_batch_cqe(ring: io_uring, cqes: MemorySegment, count: Int): Int = ???
  def io_uring_cq_advance(ring: io_uring, nr: Int): Unit = ???
  def io_uring_cq_ready(ring: io_uring) = ???
  def io_uring_prep_rw(
      op: Int,
      sqe: io_uring_sqe,
      fd: Int,
      addr: MemorySegment,
      len: Int,
      offset: Long
  ) = ???
  def io_uring_prep_nop(sqe: io_uring_sqe) = ???
  def io_uring_prep_accept(
      sqe: io_uring_sqe,
      fd: Int,
      addr: MemorySegment,
      addrlen: MemorySegment,
      flags: Int
  ) = ???
  def io_uring_prep_cancel64(sqe: io_uring_sqe, userData: Long, flags: Int) = ???
  def io_uring_prep_close(sqe: io_uring_sqe, fd: Int) = ???
  def io_uring_prep_connect(sqe: io_uring_sqe, fd: Int, addr: MemorySegment, addrlen: Int) = ???
  def io_uring_prep_recv(sqe: io_uring_sqe, sockfd: Int, buf: MemorySegment, len: Int, flags: Int) =
    ???
  def io_uring_prep_send(sqe: io_uring_sqe, sockfd: Int, buf: MemorySegment, len: Int, flags: Int) =
    ???
  def io_uring_sqe_set_data64(sqe: io_uring_sqe, data: Long) = ???
  def io_uring_cqe_get_data64(cqe: io_uring_cqe) = ???
  def io_uring_prep_shutdown(sqe: io_uring_sqe, fd: Int, how: Int) = ???
  def io_uring_prep_socket(sqe: io_uring_sqe, domain: Int, _type: Int, protocol: Int, flags: Int) =
    ???
  def io_uring_prep_timeout(sqe: io_uring_sqe, ts: __kernel_timespec, count: Int, flags: Int) = ???

}

private[uring] object uringOps {

  def io_uring_sqe_set_data[A <: AnyRef](sqe: MemorySegment, data: A): Unit = ???
  // io_uring_sqe.setUserData(sqe, castRawPtrToLong(castObjectToRawPtr(data)).toULong)

  def io_uring_cqe_get_data[A <: AnyRef](cqe: MemorySegment): A = ???
  // castRawPtrToObject(castLongToRawPtr(io_uring_sqe.getUserData(cqe).toLong)).asInstanceOf[A]

}
