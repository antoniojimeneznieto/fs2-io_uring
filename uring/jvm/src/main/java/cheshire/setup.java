package cheshire;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

class setup {

	private static int PTR_ERR(MemorySegment ptr) {
		return (int) ptr.address();
	};

	private static boolean IS_ERR(MemorySegment ptr) {
		return Long.compareUnsigned(ptr.address(), constants.EADDRESS) >= 0;
	};

	private static void io_uring_setup_ring_pointers(MemorySegment p, MemorySegment sq, MemorySegment cq) {
		MemorySegment sqRing = io_uring_sq.getRingPtr(sq);
		MemorySegment cqRing = io_uring_cq.getRingPtr(cq);
		MemorySegment sqOff = io_uring_params.getSqOffSegment(p);
		MemorySegment cqOff = io_uring_params.getCqOffSegment(p);

		io_uring_sq.setKhead(sq, utils.getSegmentWithOffset(sqRing, io_sqring_offsets.getHead(sqOff)));
		io_uring_sq.setKtail(sq, utils.getSegmentWithOffset(sqRing, io_sqring_offsets.getTail(sqOff)));
		io_uring_sq.setKringMask(sq, utils.getSegmentWithOffset(sqRing, io_sqring_offsets.getRingMask(sqOff)));
		io_uring_sq.setKringEntries(sq, utils.getSegmentWithOffset(sqRing, io_sqring_offsets.getRingEntries(sqOff)));
		io_uring_sq.setKflags(sq, utils.getSegmentWithOffset(sqRing, io_sqring_offsets.getFlags(sqOff)));
		io_uring_sq.setKdropped(sq, utils.getSegmentWithOffset(sqRing, io_sqring_offsets.getDropped(sqOff)));
		if ((io_uring_params.getFlags(p) & constants.IORING_SETUP_NO_SQARRAY) == 0) {
			io_uring_sq.setArray(sq, utils.getSegmentWithOffset(sqRing, io_sqring_offsets.getArray(sqOff)));
		}

		io_uring_cq.setKhead(cq, utils.getSegmentWithOffset(cqRing, io_cqring_offsets.getHead(cqOff)));
		io_uring_cq.setKtail(cq, utils.getSegmentWithOffset(cqRing, io_cqring_offsets.getTail(cqOff)));
		io_uring_cq.setKringMask(cq, utils.getSegmentWithOffset(cqRing, io_cqring_offsets.getRingMask(cqOff)));
		io_uring_cq.setKringEntries(cq, utils.getSegmentWithOffset(cqRing, io_cqring_offsets.getRingEntries(cqOff)));
		io_uring_cq.setKoverflow(cq, utils.getSegmentWithOffset(cqRing, io_cqring_offsets.getOverflow(cqOff)));
		io_uring_cq.setCqes(cq, utils.getSegmentWithOffset(cqRing, io_cqring_offsets.getCqes(cqOff)));
		if (io_cqring_offsets.getFlags(p) != 0) {
			io_uring_cq.setKflags(cq, utils.getSegmentWithOffset(cqRing, io_cqring_offsets.getFlags(cqOff)));
		}

		io_uring_sq.setRingMask(sq, io_uring_sq.getKringMask(sq).get(ValueLayout.JAVA_INT, 0L));
		io_uring_sq.setRingEntries(sq, io_uring_sq.getKringEntries(sq).get(ValueLayout.JAVA_INT, 0L));
		io_uring_cq.setRingMask(cq, io_uring_cq.getKringMask(cq).get(ValueLayout.JAVA_INT, 0L));
		io_uring_cq.setRingEntries(cq, io_uring_cq.getKringEntries(cq).get(ValueLayout.JAVA_INT, 0L));
	};

	private static void io_uring_unmap_rings(MemorySegment sq, MemorySegment cq) {
		long sqRingSz = io_uring_sq.getRingSz(sq);
		MemorySegment sqRingPtr = io_uring_sq.getRingPtr(sq);

		long cqRingSz = io_uring_cq.getRingSz(cq);
		MemorySegment cqRingPtr = io_uring_cq.getRingPtr(cq);

		if (sqRingSz != 0)
			syscall.__sys_munmap(sqRingPtr, sqRingSz);
		if (utils.isNotNull(cqRingPtr) && cqRingSz != 0 && !utils.areSegmentsEquals(cqRingPtr, sqRingPtr))
			syscall.__sys_munmap(cqRingPtr, cqRingSz);
	};

	private static long params_sqes_size(MemorySegment p, int sqes) {
		int flags = io_uring_params.getFlags(p);
		sqes = sqes << liburing.io_uring_sqe_shift_from_flags(flags);
		return sqes * io_uring_sqe.layout.byteSize();
	};

	private static long params_cq_size(MemorySegment p, int cqes) {
		int flags = io_uring_params.getFlags(p);
		cqes = cqes << liburing.io_uring_cqe_shift_from_flags(flags);
		return cqes * io_uring_cqe.layout.byteSize();
	};

	private static int io_uring_mmap(int fd, MemorySegment p, MemorySegment sq, MemorySegment cq) {
		int ret;

		MemorySegment sqOff = io_uring_params.getSqOffSegment(p);
		MemorySegment cqOff = io_uring_params.getCqOffSegment(p);

		io_uring_sq.setRingSz(sq, io_sqring_offsets.getArray(sqOff) + io_uring_params.getSqEntries(p) * Integer.BYTES);
		io_uring_cq.setRingSz(cq,
				io_cqring_offsets.getCqes(cqOff) + params_cq_size(p, io_uring_params.getCqEntries(p)));

		int features = io_uring_params.getFeatures(p);
		if ((features & constants.IORING_FEAT_SINGLE_MMAP) != 0) {
			long cqRingSz = io_uring_cq.getRingSz(cq);
			long sqRingSz = io_uring_sq.getRingSz(sq);
			if (cqRingSz > sqRingSz) {
				io_uring_sq.setRingSz(sq, cqRingSz);
			}
			io_uring_cq.setRingSz(cq, sqRingSz);
		}

		MemorySegment ringPtr1 = syscall.__sys_mmap(MemorySegment.ofAddress(0), io_uring_sq.getRingSz(sq),
				constants.PROT_READ | constants.PROT_WRITE, constants.MAP_SHARED | constants.MAP_POPULATE, fd,
				constants.IORING_OFF_SQ_RING);
		io_uring_sq.setRingPtr(sq, ringPtr1);
		if (IS_ERR(ringPtr1)) {
			return PTR_ERR(ringPtr1);
		}

		if ((features & constants.IORING_FEAT_SINGLE_MMAP) != 0) {
			io_uring_cq.setRingPtr(cq, io_uring_sq.getRingPtr(sq));
		} else {
			MemorySegment ringPtr2 = syscall.__sys_mmap(MemorySegment.ofAddress(0), io_uring_cq.getRingSz(cq),
					constants.PROT_READ | constants.PROT_WRITE, constants.MAP_SHARED | constants.MAP_POPULATE, fd,
					constants.IORING_OFF_CQ_RING);
			io_uring_cq.setRingPtr(sq, ringPtr2);
			if (IS_ERR(ringPtr2)) {
				ret = PTR_ERR(ringPtr2);
				io_uring_cq.ringPtrVarHandle.set(cq, MemorySegment.NULL);
				io_uring_unmap_rings(sq, cq);
				return ret;
			}
		}

		MemorySegment sqesPtr = syscall.__sys_mmap(MemorySegment.ofAddress(0),
				params_sqes_size(p, io_uring_params.getSqEntries(p)), constants.PROT_READ | constants.PROT_WRITE,
				constants.MAP_SHARED | constants.MAP_POPULATE, fd, constants.IORING_OFF_SQES);
		io_uring_sq.setSqes(sq, sqesPtr);
		if (IS_ERR(sqesPtr)) {
			ret = PTR_ERR(sqesPtr);
			io_uring_unmap_rings(sq, cq);
			return ret;
		}

		io_uring_setup_ring_pointers(p, sq, cq);
		return 0;
	};

	private static int io_uring_queue_mmap(int fd, MemorySegment p, MemorySegment ring) {
		ring.fill((byte) 0);
		MemorySegment sq = io_uring.getSqSegment(ring);
		MemorySegment cq = io_uring.getCqSegment(ring);
		return io_uring_mmap(fd, p, sq, cq);
	};

	private static long io_uring_sqes_size(MemorySegment ring) {
		MemorySegment sq = io_uring.getSqSegment(ring);
		int ringEntries = io_uring_sq.getRingEntries(sq);
		int flags = io_uring.getFlags(ring);
		return (ringEntries << liburing.io_uring_sqe_shift_from_flags(flags)) * io_uring_sqe.layout.byteSize();
	};

	private static int fls(int x) {
		if (x == 0) {
			return 0;
		}
		return Integer.SIZE - Integer.numberOfLeadingZeros(x);
	};

	private static int roundup_pow2(int depth) {
		if (depth <= 1) {
			return 1;
		}
		return 1 << fls(depth - 1);
	};

	private static int get_sq_cq_entries(int entries, MemorySegment p, MemorySegment sq, MemorySegment cq) {
		int cqEntries;
		int flags = io_uring_params.getFlags(p);

		if (entries == 0) {
			return -constants.EINVAL;
		}
		if (entries > constants.KERN_MAX_ENTRIES) {
			if ((flags & constants.IORING_SETUP_CLAMP) == 0) {
				return -constants.EINVAL;
			}
			entries = constants.KERN_MAX_ENTRIES;
		}

		entries = roundup_pow2(entries);
		if ((flags & constants.IORING_SETUP_CQSIZE) != 0) {
			int pCqEntries = io_uring_params.getCqEntries(p);
			if (pCqEntries == 0) {
				return -constants.EINVAL;
			}
			cqEntries = pCqEntries;
			if (cqEntries > constants.KERN_MAX_CQ_ENTRIES) {
				if ((flags & constants.IORING_SETUP_CLAMP) == 0) {
					return -constants.EINVAL;
				}
				cqEntries = constants.KERN_MAX_CQ_ENTRIES;
			}
			cqEntries = roundup_pow2(cqEntries);
			if (cqEntries < entries) {
				return -constants.EINVAL;
			}
		} else {
			cqEntries = 2 * entries;
		}

		sq.set(ValueLayout.JAVA_INT, 0L, entries);
		cq.set(ValueLayout.JAVA_INT, 0L, cqEntries);
		return 0;
	};

	/* FIXME */
	private static long hugePageSize = 2 * 1024 * 1024;

	private static int io_uring_alloc_huge(int entries, MemorySegment p, MemorySegment sq, MemorySegment cq,
			MemorySegment buf, long bufSize, MemorySegment sqEntries, MemorySegment cqEntries) {
		long pageSize = syscall.get_page_size();
		long ringMem, sqesMem = 0;
		long memUsed = 0;
		MemorySegment ptr;

		int ret = get_sq_cq_entries(entries, p, sqEntries, cqEntries);
		if (ret != 0) {
			return ret;
		}

		ringMem = constants.KRING_SIZE;

		int sqEntriesInt = sqEntries.get(ValueLayout.JAVA_INT, 0L);
		sqesMem = params_sqes_size(p, sqEntriesInt);
		int flags = io_uring_params.getFlags(p);
		if ((flags & constants.IORING_SETUP_NO_SQARRAY) == 0) {
			sqesMem += sqEntriesInt * Integer.BYTES;
		}
		sqesMem = (sqesMem + pageSize - 1) & ~(pageSize - 1);

		ringMem += sqesMem + params_cq_size(p, cqEntries.get(ValueLayout.JAVA_INT, 0L));
		memUsed = ringMem;
		memUsed = (memUsed + pageSize - 1) & ~(pageSize - 1);

		if (utils.isNull(buf) && (sqesMem > hugePageSize || ringMem > hugePageSize)) {
			return -constants.ENOMEM;
		}

		if (utils.isNotNull(buf)) {
			if (memUsed > bufSize) {
				return -constants.ENOMEM;
			}
			ptr = buf;
		} else {
			int mapHugetlb = 0;
			if (sqesMem <= pageSize) {
				bufSize = pageSize;
			} else {
				bufSize = hugePageSize;
				mapHugetlb = constants.MAP_HUGETLB;
			}
			ptr = syscall.__sys_mmap(null, bufSize, constants.PROT_READ | constants.PROT_WRITE,
					constants.MAP_SHARED | constants.MAP_ANONYMOUS | mapHugetlb, -1, 0);
			if (IS_ERR(ptr)) {
				return PTR_ERR(ptr);
			}
		}

		io_uring_sq.setSqes(sq, ptr);
		if (memUsed <= bufSize) {
			io_uring_sq.setRingPtr(sq, utils.getSegmentWithOffset(io_uring_sq.getSqes(sq), sqesMem));
			io_uring_cq.setRingSz(cq, 0L);
			io_uring_sq.setRingSz(sq, 0L);
		} else {
			int mapHugetlb = 0;
			if (ringMem <= pageSize) {
				bufSize = pageSize;
			} else {
				bufSize = hugePageSize;
				mapHugetlb = constants.MAP_HUGETLB;
			}
			ptr = syscall.__sys_mmap(null, bufSize, constants.PROT_READ | constants.PROT_WRITE,
					constants.MAP_SHARED | constants.MAP_ANONYMOUS | mapHugetlb, -1, 0);
			if (IS_ERR(ptr)) {
				syscall.__sys_munmap(io_uring_sq.getSqes(sq), 1);
				return PTR_ERR(ptr);
			}
			io_uring_sq.setRingPtr(sq, ptr);
			io_uring_sq.setRingSz(sq, bufSize);
			io_uring_cq.setRingSz(cq, 0);
		}

		io_uring_cq.setRingPtr(cq, io_uring_sq.getRingPtr(sq));
		MemorySegment sqOff = io_uring_params.getSqOffSegment(p);
		MemorySegment cqOff = io_uring_params.getCqOffSegment(p);
		io_sqring_offsets.setUserAddr(sqOff, io_uring_sq.getSqes(sq).address());
		io_cqring_offsets.setUserAddr(cqOff, io_uring_sq.getRingPtr(sq).address());
		return (int) memUsed;
	};

	private static int __io_uring_queue_init_params(int entries, io_uring ring, MemorySegment p, MemorySegment buf,
			long bufSize) {
		int fd = 0;
		int ret = 0;

		ring.segment.fill((byte) 0);

		MemorySegment sq = io_uring.getSqSegment(ring.segment);
		MemorySegment cq = io_uring.getCqSegment(ring.segment);
		int flags = io_uring_params.getFlags(p);

		if (((flags & constants.IORING_SETUP_REGISTERED_FD_ONLY) != 0)
				&& ((flags & constants.IORING_SETUP_NO_MMAP) == 0)) {
			return -constants.EINVAL;
		}

		if ((flags & constants.IORING_SETUP_NO_MMAP) != 0) {
			ret = io_uring_alloc_huge(entries, p, sq, cq, buf, bufSize,
					ring_allocations.getSqEntriesSegment(ring.allocations),
					ring_allocations.getCqEntriesSegment(ring.allocations));
			if (ret < 0) {
				return ret;
			}
			if (utils.isNotNull(buf)) {
				io_uring.setIntFlags(ring.segment,
						(byte) (io_uring.getIntFlags(ring.segment) | constants.INT_FLAG_APP_MEM));
			}
		}

		fd = syscall.__sys_io_uring_setup(entries, p);
		if (fd < 0) {
			if (((flags & constants.IORING_SETUP_NO_MMAP) != 0)
					&& ((io_uring.getIntFlags(ring.segment) & constants.INT_FLAG_APP_MEM) == 0)) {
				syscall.__sys_munmap(io_uring_sq.getSqes(sq), 1);
				io_uring_unmap_rings(sq, cq);
			}
			return fd;
		}

		if (((flags & constants.IORING_SETUP_NO_MMAP) == 0)) {
			ret = io_uring_queue_mmap(fd, p, ring.segment);
			if (ret != 0) {
				syscall.__sys_close(fd);
				return ret;
			}
		} else {
			io_uring_setup_ring_pointers(p, sq, cq);
		}

		if ((flags & constants.IORING_SETUP_NO_SQARRAY) == 0) {
			int sqEntries = io_uring_sq.getRingEntries(sq);
			MemorySegment sqArray = io_uring_sq.getArray(sq).reinterpret(sqEntries * ValueLayout.JAVA_INT.byteSize());
			for (int index = 0; index < sqEntries; index++) {
				sqArray.setAtIndex(ValueLayout.JAVA_INT, index, index);
			}
		}
		io_uring.setFeatures(ring.segment, io_uring_params.getFeatures(p));
		io_uring.setFlags(ring.segment, io_uring_params.getFlags(p));
		io_uring.setEnterRingFd(ring.segment, fd);
		if ((flags & constants.IORING_SETUP_REGISTERED_FD_ONLY) != 0) {
			io_uring.setRingFd(ring.segment, -1);
			io_uring.setIntFlags(ring.segment, (byte) (io_uring.getIntFlags(ring.segment) | constants.INT_FLAG_REG_RING
					| constants.INT_FLAG_REG_REG_RING));
		} else {
			io_uring.setRingFd(ring.segment, fd);
		}

		if ((flags
				& (constants.IORING_SETUP_IOPOLL | constants.IORING_SETUP_SQPOLL)) == constants.IORING_SETUP_IOPOLL) {
			io_uring.setIntFlags(ring.segment,
					(byte) (io_uring.getIntFlags(ring.segment) | constants.INT_FLAG_CQ_ENTER));
		}

		return ret;
	};

	private static int io_uring_queue_init_try_nosqarr(int entries, io_uring ring, MemorySegment p, MemorySegment buf,
			long bufSize) {
		int flags = io_uring_params.getFlags(p);

		io_uring_params.setFlags(p, flags | constants.IORING_SETUP_NO_SQARRAY);
		int ret = __io_uring_queue_init_params(entries, ring, p, buf, bufSize);

		if (ret != -constants.EINVAL || ((flags & constants.IORING_SETUP_NO_SQARRAY) != 0)) {
			return ret;
		}

		io_uring_params.setFlags(p, flags);
		return __io_uring_queue_init_params(entries, ring, p, buf, bufSize);
	};

	public static int io_uring_queue_init_params(int entries, io_uring ring, MemorySegment p) {
		int ret = io_uring_queue_init_try_nosqarr(entries, ring, p, MemorySegment.NULL, 0L);
		return ret >= 0 ? 0 : ret;
	};

	public static void io_uring_queue_exit(MemorySegment ring, MemorySegment up) {
		MemorySegment sq = io_uring.getSqSegment(ring);
		MemorySegment cq = io_uring.getCqSegment(ring);

		if ((io_uring.getIntFlags(ring) & constants.INT_FLAG_APP_MEM) == 0) {
			MemorySegment sqes = io_uring_sq.getSqes(sq);
			syscall.__sys_munmap(sqes, io_uring_sqes_size(ring));
			io_uring_unmap_rings(sq, cq);
		}

		if ((io_uring.getIntFlags(ring) & constants.INT_FLAG_REG_RING) != 0) {
			register.io_uring_unregister_ring_fd(ring, up);
		}
		int ringFd = io_uring.getRingFd(ring);
		if (ringFd != -1) {
			syscall.__sys_close(ringFd);
		}
	};

};
