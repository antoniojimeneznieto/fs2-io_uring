package cheshire;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

class sanitize {
	private static void sanitize_sqe_addr(MemorySegment sqe) {
		long addr = io_uring_sqe.getAddr(sqe);
		if (addr == 0L) {
			System.exit(1);
		}
	};

	private static void sanitize_sqe_optval(MemorySegment sqe) {
		long optval = io_uring_sqe.getOptval(sqe);
		long optlen = io_uring_sqe.getOptlen(sqe);
		if (optval == 0L || optlen == 0L) {
			System.exit(1);
		}
	};

	private static void sanitize_sqe_addr2(MemorySegment sqe) {
		long addr2 = io_uring_sqe.getAddr2(sqe);
		if (addr2 == 0L) {
			System.exit(1);
		}
	};

	private static void sanitize_sqe_addr3(MemorySegment sqe) {
		long addr3 = io_uring_sqe.getAddr3(sqe);
		if (addr3 == 0L) {
			System.exit(1);
		}
	};

	private static void sanitize_sqe_addr_and_add2(MemorySegment sqe) {
		sanitize_sqe_addr(sqe);
		sanitize_sqe_addr2(sqe);
	};

	private static void sanitize_sqe_addr_and_add3(MemorySegment sqe) {
		sanitize_sqe_addr(sqe);
		sanitize_sqe_addr3(sqe);
	};

	private static void sanitize_sqe_nop(MemorySegment sqe) {
	};

	private static Map<Integer, Consumer<MemorySegment>> sanitizeHandlers = new HashMap<>();
	private static boolean sanitizeHandlersInitialized = false;

	private static void initialize_sanitize_handlers() {
		if (sanitizeHandlersInitialized) {
			return;
		}

		sanitizeHandlers.put(constants.IORING_OP_NOP, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_READV, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_WRITEV, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FSYNC, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_READ_FIXED, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_WRITE_FIXED, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_POLL_ADD, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_POLL_REMOVE, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SYNC_FILE_RANGE, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SENDMSG, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_RECVMSG, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_TIMEOUT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_TIMEOUT_REMOVE, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_ACCEPT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_ASYNC_CANCEL, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_LINK_TIMEOUT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_CONNECT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FALLOCATE, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_OPENAT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_CLOSE, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FILES_UPDATE, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_STATX, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_READ, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_WRITE, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FADVISE, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_MADVISE, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SEND, sqe -> sanitize_sqe_addr_and_add2(sqe));
		sanitizeHandlers.put(constants.IORING_OP_RECV, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_OPENAT2, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_EPOLL_CTL, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SPLICE, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_PROVIDE_BUFFERS, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_REMOVE_BUFFERS, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_TEE, sqe -> sanitize_sqe_nop(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SHUTDOWN, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_RENAMEAT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_UNLINKAT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_MKDIRAT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SYMLINKAT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_LINKAT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_MSG_RING, sqe -> sanitize_sqe_addr_and_add3(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FSETXATTR, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SETXATTR, sqe -> sanitize_sqe_addr_and_add3(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FGETXATTR, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_GETXATTR, sqe -> sanitize_sqe_addr_and_add3(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SOCKET, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_URING_CMD, sqe -> sanitize_sqe_optval(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SEND_ZC, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_SENDMSG_ZC, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_READ_MULTISHOT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_WAITID, sqe -> sanitize_sqe_addr_and_add2(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FUTEX_WAIT, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FUTEX_WAKE, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FUTEX_WAITV, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FIXED_FD_INSTALL, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_FTRUNCATE, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_BIND, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlers.put(constants.IORING_OP_LISTEN, sqe -> sanitize_sqe_addr(sqe));
		sanitizeHandlersInitialized = true;
	};

	public static void liburing_sanitize_ring(io_uring ring) {
		MemorySegment sq = io_uring.getSqSegment(ring.segment);
		MemorySegment sqe;
		int head = liburing.io_uring_load_sq_head(ring);
		int shift = liburing.io_uring_sqe_shift(ring);

		initialize_sanitize_handlers();

		while (head != io_uring_sq.getSqeTail(sq)) {
			long offset = io_uring_sqe.layout.byteSize();
			long index = ((head & io_uring_sq.getRingMask(sq)) << shift) * offset;
			MemorySegment sqes = io_uring_sq.getSqes(sq).reinterpret(index + offset);
			sqe = sqes.asSlice(index, offset);
			byte opcode = io_uring_sqe.getOpcode(sqe);
			if (opcode < constants.IORING_OP_LAST) {
				sanitizeHandlers.get(opcode).accept(sqe);
			}
			head++;
		}
	};
}
