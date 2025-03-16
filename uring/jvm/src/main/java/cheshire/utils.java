package cheshire;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class utils {
	/*
	 * Assuming "segment == MemorySegment.NULL" (Java code) is equals to
	 * "ptr == null" or "!ptr" (C code)
	 */
	public static boolean areSegmentsEquals(MemorySegment s1, MemorySegment s2) {
		return (s1.address() == s2.address());
	};

	public static boolean isNotNull(MemorySegment s1) {
		return (s1 != null) && (s1.address() != MemorySegment.NULL.address());
	};

	public static boolean isNull(MemorySegment s1) {
		return (s1 == null) || (s1.address() == MemorySegment.NULL.address());
	};

	public static MemorySegment getSegmentWithOffset(MemorySegment s, long off) {
		return MemorySegment.ofAddress(s.address() + off).reinterpret(s.byteSize());
	};

	public static MemorySegment getIntReinterpreted(Object o) {
		return ((MemorySegment) o).reinterpret(ValueLayout.JAVA_INT.byteSize());
	};

};