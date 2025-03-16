/*
 * Copyright 2024 Arman Bilge
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

package cheshire;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/** mirrors liburing's C API */
public final class liburing {

	public static int io_uring_queue_init(int entries, io_uring ring, int flags) {
		MemorySegment params = ring_allocations.getParamsSegment(ring.allocations).fill((byte) 0);
		io_uring_params.setFlags(params, flags);
		return setup.io_uring_queue_init_params(entries, ring, params);
	};

	public static void io_uring_queue_exit(io_uring ring) {
		MemorySegment up = ring_allocations.getUpSegment(ring.allocations);
		setup.io_uring_queue_exit(ring.segment, up);
	};

	public static int io_uring_peek_batch_cqe(io_uring ring, MemorySegment cqes, int count) {
		MemorySegment countSeg = ring_allocations.getCqeCountSegment(ring.allocations);
		countSeg.set(ValueLayout.JAVA_INT, 0L, count);
		return queue.io_uring_peek_batch_cqe(ring, cqes, countSeg);
	};

	public static int io_uring_wait_cqe_timeout(io_uring ring, io_uring_cqe cqePtr, __kernel_timespec ts) {
		return queue.io_uring_wait_cqes(ring, cqePtr, 1, ts.segment, MemorySegment.NULL);
	};

	public static int io_uring_submit(io_uring ring) {
		MemorySegment flags = ring_allocations.getFlagsSegment(ring.allocations);
		return queue.__io_uring_submit_and_wait(ring, 0, flags);
	};

	public static int io_uring_submit_and_wait(io_uring ring, int waitNr) {
		MemorySegment flags = ring_allocations.getFlagsSegment(ring.allocations);
		return queue.__io_uring_submit_and_wait(ring, waitNr, flags);
	};

	public static int io_uring_submit_and_wait_timeout(io_uring ring, io_uring_cqe cqePtr, int waitNr,
			__kernel_timespec ts, MemorySegment sigmask) {
		return queue.__io_uring_submit_and_wait_timeout(ring, cqePtr, waitNr, ts.segment, 0, sigmask);
	};

	public static int io_uring_sqe_shift_from_flags(int flags) {
		return (flags & constants.IORING_SETUP_SQE128) != 0 ? 1 : 0;
	};

	public static int io_uring_sqe_shift(io_uring ring) {
		return io_uring_sqe_shift_from_flags(io_uring.getFlags(ring.segment));
	};

	public static int io_uring_cqe_shift_from_flags(int flags) {
		return (flags & constants.IORING_SETUP_CQE32) != 0 ? 1 : 0;
	};

	public static int io_uring_cqe_shift(io_uring ring) {
		return io_uring_cqe_shift_from_flags(io_uring.getFlags(ring.segment));
	};

	public static void io_uring_cq_advance(io_uring ring, int nr) {
		if (nr != 0) {
			MemorySegment cq = io_uring.getCqSegment(ring.segment);
			MemorySegment kheadSegment = io_uring_cq.getKhead(cq);
			int newValue = kheadSegment.get(ValueLayout.JAVA_INT, 0L) + nr;
			kheadSegment.set(ValueLayout.JAVA_INT, 0L, newValue);
			io_uring_cq.setReleaseKhead(cq, kheadSegment);
		}
	};

	public static void io_uring_cqe_seen(io_uring ring, io_uring_cqe cqe) {
		if (utils.isNotNull(cqe.segment)) {
			liburing.io_uring_cq_advance(ring, 1);
		}
	};

	public static void io_uring_sqe_set_data64(io_uring_sqe sqe, long data) {
		io_uring_sqe.setUserData(sqe.segment, data);
	};

	public static long io_uring_cqe_get_data64(io_uring_cqe cqe) {
		return io_uring_cqe.getUserData(cqe.segment);
	};

	private static void io_uring_initialize_sqe(MemorySegment sqe) {
		io_uring_sqe.setFlags(sqe, (byte) 0);
		io_uring_sqe.setIoprio(sqe, (short) 0);
		io_uring_sqe.setRwFlags(sqe, 0);
		io_uring_sqe.setBufIndex(sqe, (short) 0);
		io_uring_sqe.setPersonality(sqe, (short) 0);
		io_uring_sqe.setFileIndex(sqe, 0);
		io_uring_sqe.setAddr3(sqe, 0L);
		io_uring_sqe.setPad2(sqe, 0L);
	};

	public static void io_uring_prep_rw(int op, io_uring_sqe sqe, int fd, long addr, int len, long offset) {
		io_uring_sqe.setOpcode(sqe.segment, (byte) op);
		io_uring_sqe.setFd(sqe.segment, fd);
		io_uring_sqe.setOff(sqe.segment, offset);
		io_uring_sqe.setAddr(sqe.segment, addr);
		io_uring_sqe.setLen(sqe.segment, len);
	};

	public static void io_uring_prep_nop(io_uring_sqe sqe) {
		io_uring_prep_rw(constants.IORING_OP_NOP, sqe, -1, 0L, 0, 0);
	};

	public static void io_uring_prep_timeout(io_uring_sqe sqe, __kernel_timespec ts, int count, int flags) {
		io_uring_prep_rw(constants.IORING_OP_TIMEOUT, sqe, -1, ts.segment.address(), 1, count);
		io_uring_sqe.setTimeoutFlags(sqe.segment, flags);
	};

	public static void io_uring_prep_accept(io_uring_sqe sqe, int fd, long addr, long addrlen, int flags) {
		io_uring_prep_rw(constants.IORING_OP_ACCEPT, sqe, fd, addr, 0, addrlen);
		io_uring_sqe.setAcceptFlags(sqe.segment, flags);
	};

	public static void io_uring_prep_cancel64(io_uring_sqe sqe, long userData, int flags) {
		io_uring_prep_rw(constants.IORING_OP_ASYNC_CANCEL, sqe, -1, 0L, 0, 0);
		io_uring_sqe.setAddr(sqe.segment, userData);
		io_uring_sqe.setCancelFlags(sqe.segment, flags);
	};

	public static void io_uring_prep_connect(io_uring_sqe sqe, int fd, long addr, int addrlen) {
		io_uring_prep_rw(constants.IORING_OP_CONNECT, sqe, fd, addr, 0, addrlen);
	};

	public static void io_uring_prep_close(io_uring_sqe sqe, int fd) {
		io_uring_prep_rw(constants.IORING_OP_CLOSE, sqe, fd, 0L, 0, 0);
	};

	public static void io_uring_prep_send(io_uring_sqe sqe, int sockfd, long buf, int len, int flags) {
		io_uring_prep_rw(constants.IORING_OP_SEND, sqe, sockfd, buf, len, 0);
		io_uring_sqe.setMsgFlags(sqe.segment, flags);
	};

	public static void io_uring_prep_recv(io_uring_sqe sqe, int sockfd, long buf, int len, int flags) {
		io_uring_prep_rw(constants.IORING_OP_RECV, sqe, sockfd, buf, len, 0);
		io_uring_sqe.setMsgFlags(sqe.segment, flags);
	};

	public static void io_uring_prep_shutdown(io_uring_sqe sqe, int fd, int how) {
		io_uring_prep_rw(constants.IORING_OP_SHUTDOWN, sqe, fd, 0L, how, 0);
	};

	public static void io_uring_prep_socket(io_uring_sqe sqe, int domain, int type, int protocol, int flags) {
		io_uring_prep_rw(constants.IORING_OP_SOCKET, sqe, domain, 0L, protocol, type);
		io_uring_sqe.setRwFlags(sqe.segment, flags);
	};

	public static int io_uring_cq_ready(io_uring ring) {
		MemorySegment cq = io_uring.getCqSegment(ring.segment);
		int ktail = io_uring_cq.getAcquireKtail(cq).get(ValueLayout.JAVA_INT, 0L);
		int khead = io_uring_cq.getKhead(cq).get(ValueLayout.JAVA_INT, 0L);
		return (ktail - khead);
	};

	private static int io_uring_wait_cqe_nr(io_uring ring, io_uring_cqe cqePtr, int waitNr) {
		return queue.__io_uring_get_cqe(ring, cqePtr, 0, waitNr, MemorySegment.NULL);
	};

	public static int __io_uring_peek_cqe(io_uring ring, io_uring_cqe cqePtr, MemorySegment nrAvailable) {
		MemorySegment cqe;
		int err = 0;
		int available;
		MemorySegment cq = io_uring.getCqSegment(ring.segment);
		int mask = io_uring_cq.getRingMask(cq);
		int shift = io_uring_cqe_shift(ring);

		while (true) {
			int tail = io_uring_cq.getAcquireKtail(cq).get(ValueLayout.JAVA_INT, 0L);
			int head = io_uring_cq.getKhead(cq).get(ValueLayout.JAVA_INT, 0L);

			cqe = MemorySegment.NULL;
			available = tail - head;
			if (available == 0) {
				break;
			}

			long offset = io_uring_cqe.layout.byteSize();
			long index = ((head & mask) << shift) * offset;
			MemorySegment cqes = io_uring_cq.getCqes(cq).reinterpret(index + offset);
			cqe = cqes.asSlice(index, offset);
			if (((io_uring.getFeatures(ring.segment) & constants.IORING_FEAT_EXT_ARG) == 0)
					&& (io_uring_cqe.getUserData(cqe) == constants.LIBURING_UDATA_TIMEOUT)) {
				int res = io_uring_cqe.getRes(cqe);
				if (res < 0) {
					err = res;
				}
				io_uring_cq_advance(ring, 1);
				if (err == 0) {
					continue;
				}
				cqe = MemorySegment.NULL;
			}

			break;
		}

		cqePtr.segment = cqe;
		if (utils.isNotNull(nrAvailable)) {
			nrAvailable.set(ValueLayout.JAVA_INT, 0L, available);
		}
		return err;
	};

	public static int io_uring_peek_cqe(io_uring ring, io_uring_cqe cqePtr) {
		if (__io_uring_peek_cqe(ring, cqePtr, MemorySegment.NULL) == 0 && utils.isNotNull(cqePtr.segment)) {
			return 0;
		}
		return io_uring_wait_cqe_nr(ring, cqePtr, 0);
	};

	public static int io_uring_wait_cqe(io_uring ring, io_uring_cqe cqePtr) {
		if (__io_uring_peek_cqe(ring, cqePtr, MemorySegment.NULL) == 0 && utils.isNotNull(cqePtr.segment)) {
			return 0;
		}
		return io_uring_wait_cqe_nr(ring, cqePtr, 1);
	};

	public static int io_uring_load_sq_head(io_uring ring) {
		int flags = io_uring.getFlags(ring.segment);
		MemorySegment sq = io_uring.getSqSegment(ring.segment);
		if ((flags & constants.IORING_SETUP_SQPOLL) != 0) {
			return io_uring_sq.getAcquireKhead(sq).get(ValueLayout.JAVA_INT, 0L);
		}
		return io_uring_sq.getKhead(sq).get(ValueLayout.JAVA_INT, 0L);
	}

	public static MemorySegment io_uring_get_sqe(io_uring ring) {
		MemorySegment sq = io_uring.getSqSegment(ring.segment);
		int head = io_uring_load_sq_head(ring);
		int tail = io_uring_sq.getSqeTail(sq);

		if ((tail - head) >= io_uring_sq.getRingEntries(sq)) {
			return MemorySegment.NULL;
		}

		int sqeTail = io_uring_sq.getSqeTail(sq);
		int ringMask = io_uring_sq.getRingMask(sq);

		long offset = io_uring_sqe.layout.byteSize();
		long index = ((sqeTail & ringMask) << io_uring_sqe_shift(ring)) * offset;

		MemorySegment sqes = io_uring_sq.getSqes(sq).reinterpret(index + offset);
		MemorySegment sqe = sqes.asSlice(index, offset);
		io_uring_sq.setSqeTail(sq, tail + 1);
		io_uring_initialize_sqe(sqe);
		return sqe;
	};

};
