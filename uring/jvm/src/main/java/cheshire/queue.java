package cheshire;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

class queue {

	private static boolean sq_ring_needs_enter(MemorySegment ring, int submit, MemorySegment flags) {
		if (submit == 0) {
			return false;
		}

		if ((io_uring.getFlags(ring) & constants.IORING_SETUP_SQPOLL) == 0) {
			return true;
		}

		// io_uring_smp_mb();
		// std::atomic_thread_fence(std::memory_order_seq_cst);
		// Review inside if -> uring_unlikely(IO_URING_READ_ONCE(*ring->sq.kflags)
		int kflags = io_uring_sq.getAcquireKflags(io_uring.getSqSegment(ring)).get(ValueLayout.JAVA_INT, 0L);
		if ((kflags & constants.IORING_SQ_NEED_WAKEUP) != 0) {
			flags.set(ValueLayout.JAVA_INT, 0L,
					(flags.get(ValueLayout.JAVA_INT, 0L) | constants.IORING_ENTER_SQ_WAKEUP));
			return true;
		}

		return false;
	};

	private static int _io_uring_get_cqe(io_uring ring, io_uring_cqe cqePtr, MemorySegment data) {
		MemorySegment cqe = MemorySegment.NULL;
		boolean looped = false;
		int err = 0;

		MemorySegment cqeFlags = ring_allocations.getCqeFlagsSegment(ring.allocations);
		MemorySegment nrAvailable = ring_allocations.getNrAvailableSegment(ring.allocations);
		io_uring_cqe cqePtrAux = new io_uring_cqe(cqe);

		while (true) {
			boolean needEnter = false;
			cqeFlags.set(ValueLayout.JAVA_INT, 0L, io_uring.getIntFlags(ring.segment) & constants.INT_FLAGS_MASK);
			nrAvailable.set(ValueLayout.JAVA_INT, 0L, 0);
			int ret;

			ret = liburing.__io_uring_peek_cqe(ring, cqePtrAux, nrAvailable);
			if (ret != 0) {
				if (err == 0) {
					err = ret;
				}
				break;
			}

			int waitNr = get_data.getWaitNr(data);
			int submit = get_data.getSubmit(data);
			boolean hasTs = get_data.getHasTs(data) == 1;
			long sz = get_data.getSz(data);
			if (utils.isNull(cqePtrAux.segment) && waitNr == 0 && submit == 0) {
				if (looped || !cq_ring_needs_enter(ring.segment)) {
					if (err == 0) {
						err = -constants.EAGAIN;
					}
					break;
				}
				needEnter = true;
			}
			if ((waitNr > nrAvailable.get(ValueLayout.JAVA_INT, 0L)) || needEnter) {
				cqeFlags.set(ValueLayout.JAVA_INT, 0L, cqeFlags.get(ValueLayout.JAVA_INT, 0L)
						| constants.IORING_ENTER_GETEVENTS | get_data.getGetFlags(data));
				needEnter = true;
			}
			if (sq_ring_needs_enter(ring.segment, submit, cqeFlags)) {
				needEnter = true;
			}
			if (!needEnter) {
				break;
			}
			MemorySegment arg = get_data.getArg(data).reinterpret(io_uring_getevents_arg.layout.byteSize());
			if (looped && hasTs) {
				long ts = io_uring_getevents_arg.getTs(arg);
				if (utils.isNull(cqePtrAux.segment) && ts != 0 && err == 0) {
					err = -constants.ETIME;
				}
				break;
			}

			ret = syscall.__sys_io_uring_enter2(io_uring.getEnterRingFd(ring.segment), submit, waitNr,
					cqeFlags.get(ValueLayout.JAVA_INT, 0L), arg, sz);
			if (ret < 0) {
				if (err == 0) {
					err = ret;
				}
				break;
			}

			get_data.setSubmit(data, get_data.getSubmit(data) - ret);
			if (utils.isNotNull(cqe)) {
				break;
			}
			if (!looped) {
				looped = true;
				err = ret;
			}
		}

		cqePtr.segment = cqePtrAux.segment;
		return err;
	};

	public static int __io_uring_get_cqe(io_uring ring, io_uring_cqe cqePtr, int submit, int waitNr,
			MemorySegment sigmask) {
		MemorySegment data = ring_allocations.getDataSegment(ring.allocations);
		get_data.setSubmit(data, submit);
		get_data.setWaitNr(data, waitNr);
		get_data.setGetFlags(data, 0);
		get_data.setSz(data, constants._NSIG / 8);
		get_data.setArg(data, sigmask);
		return _io_uring_get_cqe(ring, cqePtr, data);
	};

	private static int io_uring_get_events(MemorySegment ring) {
		int flags = constants.IORING_ENTER_GETEVENTS | (io_uring.getIntFlags(ring) & constants.INT_FLAGS_MASK);
		return syscall.__sys_io_uring_enter(io_uring.getEnterRingFd(ring), 0, 0, flags, MemorySegment.NULL);
	};

	private static int __io_uring_flush_sq(MemorySegment ring) {
		MemorySegment sq = io_uring.getSqSegment(ring);
		int tail = io_uring_sq.getSqeTail(sq);

		if (io_uring_sq.getSqeHead(sq) != tail) {
			io_uring_sq.setSqeHead(sq, tail);

			MemorySegment ktail = io_uring_sq.getKtail(sq);
			if ((io_uring.getFlags(ring) & constants.IORING_SETUP_SQPOLL) == 0) {
				ktail.set(ValueLayout.JAVA_INT, 0L, tail);
			} else {
				// Review workaround
				ktail.set(ValueLayout.JAVA_INT, 0L, tail);
				io_uring_sq.setReleaseKtail(sq, ktail);
			}
		}
		// return tail - IO_URING_READ_ONCE(*sq->khead);
		return tail - io_uring_sq.getAcquireKhead(sq).get(ValueLayout.JAVA_INT, 0L);
	};

	private static boolean cq_ring_needs_flush(MemorySegment ring) {
		MemorySegment sq = io_uring.getSqSegment(ring);
		// IO_URING_READ_ONCE(*ring->sq.kflags) // std::memory_order_relaxed
		int kflags = io_uring_sq.getAcquireKflags(sq).get(ValueLayout.JAVA_INT, 0L);
		return ((kflags & (constants.IORING_SQ_CQ_OVERFLOW | constants.IORING_SQ_TASKRUN)) != 0);
	};

	private static boolean cq_ring_needs_enter(MemorySegment ring) {
		return ((io_uring.getIntFlags(ring) & constants.INT_FLAG_CQ_ENTER) != 0) || cq_ring_needs_flush(ring);
	};

	private static int io_uring_wait_cqes_new(io_uring ring, io_uring_cqe cqePtr, int waitNr, MemorySegment ts,
			int minWaitUsec, MemorySegment sigmask) {
		MemorySegment arg = ring_allocations.getArgSegment(ring.allocations);
		MemorySegment data = ring_allocations.getDataSegment(ring.allocations);

		io_uring_getevents_arg.setSigmask(arg, sigmask.address());
		io_uring_getevents_arg.setSigmaskSz(arg, constants._NSIG / 8);
		io_uring_getevents_arg.setTs(arg, ts.address());

		get_data.setWaitNr(data, waitNr);
		get_data.setGetFlags(data, constants.IORING_ENTER_EXT_ARG);
		get_data.setSz(data, (int) arg.byteSize());
		get_data.setHasTs(data, utils.isNotNull(ts) ? 1 : 0);
		get_data.setArg(data, arg);
		return _io_uring_get_cqe(ring, cqePtr, data);
	};

	private static int __io_uring_submit_timeout(io_uring ring, int waitNr, MemorySegment ts) {
		int ret;
		MemorySegment sqe = liburing.io_uring_get_sqe(ring);
		if (utils.isNull(sqe)) {
			ret = liburing.io_uring_submit(ring);
			if (ret < 0) {
				return ret;
			}
			sqe = liburing.io_uring_get_sqe(ring);
			if (utils.isNull(sqe)) {
				return -constants.EAGAIN;
			}
		}
		liburing.io_uring_prep_timeout(new io_uring_sqe(sqe), new __kernel_timespec(ts), waitNr, 0);
		io_uring_sqe.setUserData(sqe, constants.LIBURING_UDATA_TIMEOUT);
		return __io_uring_flush_sq(ring.segment);
	};

	private static int __io_uring_submit(io_uring ring, int submitted, int waitNr, boolean getEvents,
			MemorySegment flags) {
		boolean cqNeedsEnter = getEvents || waitNr != 0 || cq_ring_needs_enter(ring.segment);
		flags.set(ValueLayout.JAVA_INT, 0L, io_uring.getIntFlags(ring.segment) & constants.INT_FLAGS_MASK);
		int ret;

		sanitize.liburing_sanitize_ring(ring);

		if (sq_ring_needs_enter(ring.segment, submitted, flags) || cqNeedsEnter) {
			if (cqNeedsEnter) {
				flags.set(ValueLayout.JAVA_INT, 0L,
						flags.get(ValueLayout.JAVA_INT, 0L) | constants.IORING_ENTER_GETEVENTS);
			}
			ret = syscall.__sys_io_uring_enter(io_uring.getEnterRingFd(ring.segment), submitted, waitNr,
					flags.get(ValueLayout.JAVA_INT, 0L), MemorySegment.NULL);
		} else {
			ret = submitted;
		}
		return ret;
	};

	public static int io_uring_wait_cqes(io_uring ring, io_uring_cqe cqePtr, int waitNr, MemorySegment ts,
			MemorySegment sigmask) {
		int toSubmit = 0;
		if (utils.isNotNull(ts)) {
			if ((io_uring.getFeatures(ring.segment) & constants.IORING_FEAT_EXT_ARG) != 0) {
				return io_uring_wait_cqes_new(ring, cqePtr, waitNr, ts, 0, sigmask);
			}
			toSubmit = __io_uring_submit_timeout(ring, waitNr, ts);
			if (toSubmit < 0) {
				return toSubmit;
			}
		}
		return __io_uring_get_cqe(ring, cqePtr, toSubmit, waitNr, sigmask);
	};

	public static int __io_uring_submit_and_wait_timeout(io_uring ring, io_uring_cqe cqePtr, int waitNr,
			MemorySegment ts, int minWait, MemorySegment sigmask) {
		int toSubmit;

		if (utils.isNotNull(ts)) {
			if ((io_uring.getFeatures(ring.segment) & constants.IORING_FEAT_EXT_ARG) != 0) {
				MemorySegment arg = ring_allocations.getArgSegment(ring.allocations);
				MemorySegment data = ring_allocations.getDataSegment(ring.allocations);
				io_uring_getevents_arg.setSigmask(arg, sigmask.address());
				io_uring_getevents_arg.setSigmaskSz(arg, constants._NSIG / 8);
				io_uring_getevents_arg.setMinWait(arg, minWait);
				io_uring_getevents_arg.setTs(arg, ts.address());

				get_data.setSubmit(data, __io_uring_flush_sq(ring.segment));
				get_data.setWaitNr(data, waitNr);
				get_data.setGetFlags(data, constants.IORING_ENTER_EXT_ARG);
				get_data.setSz(data, (int) arg.byteSize());
				get_data.setHasTs(data, utils.isNotNull(ts) ? 1 : 0);
				get_data.setArg(data, arg);
				return _io_uring_get_cqe(ring, cqePtr, data);
			}
			toSubmit = __io_uring_submit_timeout(ring, waitNr, ts);
			if (toSubmit < 0) {
				return toSubmit;
			}
		} else {
			toSubmit = __io_uring_flush_sq(ring.segment);
		}
		return __io_uring_get_cqe(ring, cqePtr, toSubmit, waitNr, sigmask);
	};

	public static int __io_uring_submit_and_wait(io_uring ring, int waitNr, MemorySegment flags) {
		return __io_uring_submit(ring, __io_uring_flush_sq(ring.segment), waitNr, false, flags);
	};

	private static boolean io_uring_peek_batch_cqe_(io_uring ring, MemorySegment cqes, int count) {
		int ready = liburing.io_uring_cq_ready(ring);

		if (ready == 0) {
			return false;
		}
		MemorySegment cq = io_uring.getCqSegment(ring.segment);
		int shift = liburing.io_uring_cqe_shift(ring);
		int head = io_uring_cq.getKhead(cq).get(ValueLayout.JAVA_INT, 0L);
		int mask = io_uring_cq.getRingMask(cq);
		if (ready < count) {
			count = ready;
		}
		int last = head + count;
		long offset = io_uring_cqe.layout.byteSize();
		MemorySegment cqesSegment = io_uring_cq.getCqes(cq).reinterpret((last + 1) * offset); // Enough?;
		for (int i = 0; head != last; head++, i++) {
			long index = ((head & mask) << shift) * offset;
			cqes.asSlice(i * offset, offset).copyFrom(cqesSegment.asSlice(index, offset));
		}

		return true;
	};

	public static int io_uring_peek_batch_cqe(io_uring ring, MemorySegment cqes, int count) {
		// TODO: Make count a pointer
		if (io_uring_peek_batch_cqe_(ring, cqes, count)) {
			return count;
		}

		if (!cq_ring_needs_flush(ring.segment)) {
			return 0;
		}

		io_uring_get_events(ring.segment);
		if (!io_uring_peek_batch_cqe_(ring, cqes, count)) {
			return 0;
		}

		return count;
	};

};
