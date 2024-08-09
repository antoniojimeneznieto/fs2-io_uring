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

case class MemorySegment() {
  def address(): Long = 0L
}

case class io_uring() {
  val segment: MemorySegment = new MemorySegment()
  def getRingFd(ring: MemorySegment): Int = 0
}

case class io_uring_cqe() {
  val segment: MemorySegment = new MemorySegment()
  def getRes(cqe: MemorySegment): Int = 0
  def getUserData(sqe: MemorySegment): Long = 0L
}

case class io_uring_cqes(length: Int) {
  val segment: MemorySegment = new MemorySegment()
  def getCqeAtIndex(cqes: MemorySegment, index: Int): MemorySegment = new MemorySegment()
}

case class io_uring_sqe() {
  val segment: MemorySegment = new MemorySegment()
  def setOpcode(sqe: MemorySegment, value: Byte): Unit = ()
  def setFlags(sqe: MemorySegment, value: Byte): Unit = ()
  def setFd(sqe: MemorySegment, value: Int): Unit = ()
  def setOff(sqe: MemorySegment, value: Long): Unit = ()
  def setAddr(sqe: MemorySegment, value: Long): Unit = ()
  def setRwFlags(sqe: MemorySegment, value: Int): Unit = ()
  def getUserData(sqe: MemorySegment): Long = 0L
  def setUserData(sqe: MemorySegment, value: Long): Unit = ()
}

case class __kernel_timespec() {
  val segment: MemorySegment = new MemorySegment()
  def setTvSec(ts: MemorySegment, value: Long): Unit = ()
  def setTvNsec(ts: MemorySegment, value: Long): Unit = ()
}
