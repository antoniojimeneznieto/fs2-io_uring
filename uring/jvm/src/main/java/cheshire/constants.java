package cheshire;

public final class constants {

	public static final int KRING_SIZE = 64;

	public static final int IORING_ENTER_GETEVENTS = 1 << 0;
	public static final int IORING_ENTER_SQ_WAKEUP = 1 << 1;
	public static final int IORING_ENTER_EXT_ARG = 1 << 3;
	public static final int IORING_ENTER_REGISTERED_RING = 1 << 4;

	public static final int INT_FLAGS_MASK = IORING_ENTER_REGISTERED_RING;
	public static final int INT_FLAG_REG_RING = IORING_ENTER_REGISTERED_RING;
	public static final int INT_FLAG_REG_REG_RING = 1;
	public static final int INT_FLAG_APP_MEM = 2;
	public static final int INT_FLAG_CQ_ENTER = 4;

	public static final int IORING_FEAT_SINGLE_MMAP = 1 << 0;
	public static final int IORING_FEAT_EXT_ARG = 1 << 8;

	public static final long IORING_OFF_SQ_RING = 0L;
	public static final long IORING_OFF_CQ_RING = 0x8000000L;
	public static final long IORING_OFF_SQES = 0x10000000L;

	public static final int IORING_OP_NOP = 0;
	public static final int IORING_OP_READV = 1;
	public static final int IORING_OP_WRITEV = 2;
	public static final int IORING_OP_FSYNC = 3;
	public static final int IORING_OP_READ_FIXED = 4;
	public static final int IORING_OP_WRITE_FIXED = 5;
	public static final int IORING_OP_POLL_ADD = 6;
	public static final int IORING_OP_POLL_REMOVE = 7;
	public static final int IORING_OP_SYNC_FILE_RANGE = 8;
	public static final int IORING_OP_SENDMSG = 9;
	public static final int IORING_OP_RECVMSG = 10;
	public static final int IORING_OP_TIMEOUT = 11;
	public static final int IORING_OP_TIMEOUT_REMOVE = 12;
	public static final int IORING_OP_ACCEPT = 13;
	public static final int IORING_OP_ASYNC_CANCEL = 14;
	public static final int IORING_OP_LINK_TIMEOUT = 15;
	public static final int IORING_OP_CONNECT = 16;
	public static final int IORING_OP_FALLOCATE = 17;
	public static final int IORING_OP_OPENAT = 18;
	public static final int IORING_OP_CLOSE = 19;
	public static final int IORING_OP_FILES_UPDATE = 20;
	public static final int IORING_OP_STATX = 21;
	public static final int IORING_OP_READ = 22;
	public static final int IORING_OP_WRITE = 23;
	public static final int IORING_OP_FADVISE = 24;
	public static final int IORING_OP_MADVISE = 25;
	public static final int IORING_OP_SEND = 26;
	public static final int IORING_OP_RECV = 27;
	public static final int IORING_OP_OPENAT2 = 28;
	public static final int IORING_OP_EPOLL_CTL = 29;
	public static final int IORING_OP_SPLICE = 30;
	public static final int IORING_OP_PROVIDE_BUFFERS = 31;
	public static final int IORING_OP_REMOVE_BUFFERS = 32;
	public static final int IORING_OP_TEE = 33;
	public static final int IORING_OP_SHUTDOWN = 34;
	public static final int IORING_OP_RENAMEAT = 35;
	public static final int IORING_OP_UNLINKAT = 36;
	public static final int IORING_OP_MKDIRAT = 37;
	public static final int IORING_OP_SYMLINKAT = 38;
	public static final int IORING_OP_LINKAT = 39;
	public static final int IORING_OP_MSG_RING = 40;
	public static final int IORING_OP_FSETXATTR = 41;
	public static final int IORING_OP_SETXATTR = 42;
	public static final int IORING_OP_FGETXATTR = 43;
	public static final int IORING_OP_GETXATTR = 44;
	public static final int IORING_OP_SOCKET = 45;
	public static final int IORING_OP_URING_CMD = 46;
	public static final int IORING_OP_SEND_ZC = 47;
	public static final int IORING_OP_SENDMSG_ZC = 48;
	public static final int IORING_OP_READ_MULTISHOT = 49;
	public static final int IORING_OP_WAITID = 50;
	public static final int IORING_OP_FUTEX_WAIT = 51;
	public static final int IORING_OP_FUTEX_WAKE = 52;
	public static final int IORING_OP_FUTEX_WAITV = 53;
	public static final int IORING_OP_FIXED_FD_INSTALL = 54;
	public static final int IORING_OP_FTRUNCATE = 55;
	public static final int IORING_OP_BIND = 56;
	public static final int IORING_OP_LISTEN = 57;
	public static final int IORING_OP_RECV_ZC = 58;
	public static final int IORING_OP_EPOLL_WAIT = 59;
	public static final int IORING_OP_READV_FIXED = 60;
	public static final int IORING_OP_WRITEV_FIXED = 61;
	public static final int IORING_OP_LAST = 62;

	public static final int IORING_REGISTER_USE_REGISTERED_RING = 1 << 31;
	public static final int IORING_UNREGISTER_RING_FDS = 21;

	public static final int IORING_SETUP_IOPOLL = 1 << 0;
	public static final int IORING_SETUP_SQPOLL = 1 << 1;
	public static final int IORING_SETUP_CQSIZE = 1 << 3;
	public static final int IORING_SETUP_CLAMP = 1 << 4;
	public static final int IORING_SETUP_SUBMIT_ALL = 1 << 7;
	public static final int IORING_SETUP_COOP_TASKRUN = 1 << 8;
	public static final int IORING_SETUP_TASKRUN_FLAG = 1 << 9;
	public static final int IORING_SETUP_SQE128 = 1 << 10;
	public static final int IORING_SETUP_CQE32 = 1 << 11;
	public static final int IORING_SETUP_SINGLE_ISSUER = 1 << 12;
	public static final int IORING_SETUP_DEFER_TASKRUN = 1 << 13;
	public static final int IORING_SETUP_NO_MMAP = 1 << 14;
	public static final int IORING_SETUP_REGISTERED_FD_ONLY = 1 << 15;
	public static final int IORING_SETUP_NO_SQARRAY = 1 << 16;

	public static final int IORING_SQ_NEED_WAKEUP = 1 << 0;
	public static final int IORING_SQ_CQ_OVERFLOW = 1 << 1;
	public static final int IORING_SQ_TASKRUN = 1 << 2;

	public static final int KERN_MAX_ENTRIES = 32768;
	public static final int KERN_MAX_CQ_ENTRIES = (2 * KERN_MAX_ENTRIES);

	public static final long LIBURING_UDATA_TIMEOUT = 0xFFFFFFFFFFFFFFFFL;

	public static final int MAP_ANONYMOUS = 0x20;
	public static final int MAP_HUGETLB = 0x0040000;
	public static final int MAP_POPULATE = 0x8000;
	public static final int MAP_SHARED = 0x01;

	public static final int PROT_READ = 0x1;
	public static final int PROT_WRITE = 0x2;

	public static final int EAGAIN = 11;
	public static final int ENOMEM = 12;
	public static final int EINVAL = 22;
	public static final int ETIME = 62;
	public static final long MAP_FAILED = -1L;
	public static final long EADDRESS = -4095L;

	public static final int _NSIG;
	public static final int __NR_io_uring_setup;
	public static final int __NR_io_uring_enter;
	public static final int __NR_io_uring_register;
	public static final int __NR_Linux = 4000;

	static {
		String arch = System.getProperty("os.arch");
		if ("alpha".equals(arch)) {
			__NR_io_uring_setup = 535;
			__NR_io_uring_enter = 536;
			__NR_io_uring_register = 537;
			_NSIG = 32;
		} else if ("mips".equals(arch)) {
			__NR_io_uring_setup = __NR_Linux + 425;
			__NR_io_uring_enter = __NR_Linux + 426;
			__NR_io_uring_register = __NR_Linux + 427;
			_NSIG = 128;
		} else {
			__NR_io_uring_setup = 425;
			__NR_io_uring_enter = 426;
			__NR_io_uring_register = 427;
			_NSIG = 65;
		}
	}
}
