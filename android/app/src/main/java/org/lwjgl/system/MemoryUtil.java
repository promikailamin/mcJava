package org.lwjgl.system;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Native memory emulation for Android. Real malloc is unavailable (no JNI glue yet),
 * so every "native address" is a handle registered against a {@link ByteBuffer#allocateDirect}
 * arena. All address-taking GL calls resolve handles through {@link #region(long)}.
 *
 * This is the single choke point a future NDK-backed allocator can replace transparently.
 */
public final class MemoryUtil {

    public interface MemoryAllocator {
        long malloc(long size);

        long calloc(long num, long size);

        long realloc(long address, long size);

        void free(long address);
    }

    private static final MemoryAllocator DEFAULT_ALLOCATOR = new MemoryAllocator() {
        @Override
        public long malloc(long size) {
            return nalloc(size);
        }

        @Override
        public long calloc(long num, long size) {
            return ncalloc(num, size);
        }

        @Override
        public long realloc(long address, long size) {
            return nrealloc(address, size);
        }

        @Override
        public void free(long address) {
            nfree(address);
        }
    };

    public static MemoryAllocator getAllocator(boolean direct) {
        return DEFAULT_ALLOCATOR;
    }

    public static final class memory_allocator {
        public long malloc(long size) { return nalloc(size); }
        public long calloc(long num, long size) { return MemoryUtil.ncalloc(num, size); }
        public long realloc(long address, long size) { return MemoryUtil.nrealloc(address, size); }
        public void free(long address) { MemoryUtil.nfree(address); }
    }

    /** A slice of a direct byte arena. */
    public record Region(long base, ByteBuffer parent, int offset, int length) {
        public ByteBuffer bytes() {
            ByteBuffer slice = parent.duplicate().order(ByteOrder.nativeOrder());
            slice.position(offset);
            slice.limit(offset + length);
            return slice.slice();
        }
    }

    private static final AtomicLong NEXT_HANDLE = new AtomicLong(0x1000L);
    private static final NavigableMap<Long, Region> REGIONS = new TreeMap<>();
    private static final ConcurrentHashMap<Buffer, Long> SLICES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Buffer, Long> HANDLED = new ConcurrentHashMap<>();

    private static final boolean DEBUG = Boolean.parseBoolean(
            System.getProperty("org.lwjgl.util.Debug", "false"));

    private MemoryUtil() {
    }

    public static memory_allocator getAllocator() {
        return new memory_allocator();
    }

    // ---------------------------------------------------------------- allocation

    public static long nalloc(long size) {
        long base = NEXT_HANDLE.incrementAndGet();
        int len = (int) Math.max(1L, size);
        ByteBuffer parent = ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
        REGIONS.put(base, new Region(base, parent, 0, len));
        return base;
    }

    public static long ncalloc(long num, long size) {
        long address = nalloc(num * size);
        ByteBuffer b = region(address).bytes();
        while (b.hasRemaining()) {
            b.put((byte) 0);
        }
        return address;
    }

    public static long nmemAlloc(long size) {
        return nalloc(size);
    }

    public static long nmemCalloc(long num, long size) {
        return ncalloc(num, size);
    }

    public static long nmemRealloc(long address, long size) {
        Region old = region(address);
        long newAddress = nalloc(size);
        ByteBuffer src = old.bytes();
        ByteBuffer dst = region(newAddress).bytes();
        int copy = Math.min(src.capacity(), dst.capacity());
        src.limit(copy);
        dst.put(src);
        nfree(address);
        return newAddress;
    }

    public static long nrealloc(long address, long size) {
        return nmemRealloc(address, size);
    }

    public static void nfree(long address) {
        REGIONS.remove(address);
    }

    public static void nmemFree(long address) {
        nfree(address);
    }

    public static void memFree(long address) {
        nfree(address);
    }

    public static void memFree(Buffer buffer) {
        Long address = HANDLED.remove(buffer);
        if (address != null) {
            REGIONS.remove(address);
        }
    }

    // ---------------------------------------------------------------- addressing

    public static long memAddress(ByteBuffer buffer) {
        return addressOf(buffer);
    }

    public static long memAddress(Buffer buffer) {
        return addressOf(buffer);
    }

    public static long memAddress0(Buffer buffer) {
        return addressOf(buffer);
    }

    public static long memAddress0(ByteBuffer buffer) {
        return addressOf(buffer);
    }

    private static long addressOf(Buffer buffer) {
        Long cached = SLICES.get(buffer);
        if (cached != null) {
            return cached;
        }
        long base = NEXT_HANDLE.incrementAndGet();
        int len = Math.max(1, buffer.capacity() * 4);
        ByteBuffer parent = buffer instanceof ByteBuffer bb
                ? bb.duplicate().order(ByteOrder.nativeOrder())
                : ByteBuffer.allocateDirect(len).order(ByteOrder.nativeOrder());
        REGIONS.put(base, new Region(base, parent, 0, len));
        HANDLED.put(buffer, base);
        return base;
    }

    static Region regionOrNull(long address) {
        if (address == 0L) {
            return null;
        }
        return REGIONS.floorEntry(address).getValue();
    }

    /** Resolve an address to the nearest mapped arena (throws on garbage handles). */
    public static Region region(long address) {
        Region r = regionOrNull(address);
        if (r == null) {
            throw new NullPointerException("invalid native address: 0x" + Long.toHexString(address));
        }
        return r;
    }

    /** Resolve an address taking an optional byte offset into account (address = base + off). */
    public static ByteBuffer byteBuffer(long address, int length) {
        Region r = region(address);
        int off = (int) (address - r.base());
        int end = Math.min(r.offset() + off + Math.max(0, length), r.offset() + r.length());
        ByteBuffer slice = r.parent().duplicate().order(ByteOrder.nativeOrder());
        slice.position(r.offset() + off);
        slice.limit(end);
        return slice.slice();
    }

    static long rawOffset(long address) {
        Region r = region(address);
        return r.offset() + (address - r.base());
    }

    // ---------------------------------------------------------------- buffer views

    public static ByteBuffer memByteBuffer(long address, int size) {
        ByteBuffer result = byteBuffer(address, size);
        SLICES.put(result, address);
        return result;
    }

    public static ByteBuffer memUTF8(CharSequence text) {
        return java.nio.charset.StandardCharsets.UTF_8.encode(text.toString());
    }

    /** Encodes {@code text} into a native (shim) allocation; {@code nullTerminated} appends a NUL. */
    public static long memUTF8(CharSequence text, boolean nullTerminated) {
        if (text == null) {
            return 0L;
        }
        java.nio.ByteBuffer enc = java.nio.charset.StandardCharsets.UTF_8.encode(text.toString());
        byte[] data = new byte[enc.remaining()];
        enc.get(data);
        long ptr = ncalloc(data.length + (nullTerminated ? 1 : 0), 1);
        memPutBytes(ptr, data, 0, data.length);
        return ptr;
    }

    /** @return the string description of a "native" message pointer (handled by the stubs themselves). */
    public static String memUTF8(long address) {
        return "0x" + Long.toHexString(address);
    }

    public static ByteBuffer memASCII(CharSequence text) {
        return java.nio.charset.StandardCharsets.US_ASCII.encode(text.toString());
    }

    /** @return the string described by the stubs at {@code address} (0 bytes owned) or "". */
    public static String memASCII(long address, int length) {
        return length <= 0 ? "" : "0x" + Long.toHexString(address);
    }

    public static String memASCII(long address) {
        return "0x" + Long.toHexString(address);
    }

    public static ByteBuffer memAlloc(int size) {
        return memByteBuffer(nalloc(size), Math.max(1, size));
    }

    public static ByteBuffer memCalloc(int size) {
        ByteBuffer buf = memAlloc(size);
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, (byte) 0);
        }
        return buf;
    }

    public static IntBuffer memAllocInt(int count) {
        int len = Math.max(1, count);
        long base = nalloc((long) len * Integer.BYTES);
        IntBuffer buf = region(base).bytes().asIntBuffer();
        buf.limit(len);
        SLICES.put(buf, base);
        return buf;
    }

    public static IntBuffer memIntBuffer(long address, int size) {
        IntBuffer buf = byteBuffer(address, size * Integer.BYTES).asIntBuffer();
        SLICES.put(buf, address);
        return buf;
    }

    public static IntBuffer memCallocInt(int count) {
        IntBuffer buf = memAllocInt(count);
        for (int i = 0; i < buf.capacity(); i++) {
            buf.put(i, 0);
        }
        return buf;
    }

    public static LongBuffer memAllocPointer(int count) {
        int len = Math.max(1, count);
        long base = nalloc((long) len * Long.BYTES);
        LongBuffer buf = region(base).bytes().asLongBuffer();
        buf.limit(len);
        SLICES.put(buf, base);
        return buf;
    }

    public static ByteBuffer memSlice(ByteBuffer buffer) {
        ByteBuffer slice = buffer.duplicate().order(ByteOrder.nativeOrder());
        SLICES.put(slice, memAddress(buffer));
        return slice;
    }

    public static ByteBuffer memSlice(ByteBuffer buffer, int offset, int length) {
        Long base = SLICES.get(buffer);
        if (base == null) {
            base = addressOf(buffer);
        }
        ByteBuffer slice = buffer.duplicate().order(ByteOrder.nativeOrder())
                .position(offset).limit(offset + length).slice();
        SLICES.put(slice, base + offset);
        return slice;
    }

    // ---------------------------------------------------------------- primitives

    public static byte memGetByte(long address) {
        return byteBuffer(address, 1).get(0);
    }

    public static short memGetShort(long address) {
        return byteBuffer(address, 2).getShort(0);
    }

    public static int memGetInt(long address) {
        return byteBuffer(address, 4).getInt(0);
    }

    public static long memGetAddress(long address) {
        return byteBuffer(address, 8).getLong(0);
    }

    public static long memGetLong(long address) {
        return byteBuffer(address, 8).getLong(0);
    }

    public static float memGetFloat(long address) {
        return byteBuffer(address, 4).getFloat(0);
    }

    public static void memPutByte(long address, byte value) {
        byteBuffer(address, 1).put(0, value);
    }

    public static void memPutShort(long address, short value) {
        byteBuffer(address, 2).putShort(0, value);
    }

    public static void memPutInt(long address, int value) {
        byteBuffer(address, 4).putInt(0, value);
    }

    public static void memPutLong(long address, long value) {
        byteBuffer(address, 8).putLong(0, value);
    }

    public static void memPutAddress(long address, long value) {
        byteBuffer(address, 8).putLong(0, value);
    }

    public static void memPutFloat(long address, float value) {
        byteBuffer(address, 4).putFloat(0, value);
    }

    public static void memPutBytes(long address, byte[] src, int offset, int length) {
        byteBuffer(address, length).put(src, offset, length);
    }

    public static void memCopy(long src, long dst, long count) {
        Region sr = region(src);
        Region dr = region(dst);
        int sc = (int) Math.min(count, sr.length());
        int dc = (int) Math.min(sc, dr.length());
        ByteBuffer s = sr.bytes();
        ByteBuffer d = dr.bytes();
        s.limit(sc);
        d.put((ByteBuffer) s.slice().limit(dc));
    }

    public static void memCopy(ByteBuffer src, long dst, long count) {
        Region dr = region(dst);
        ByteBuffer s = src.duplicate();
        ByteBuffer d = dr.bytes();
        int n = (int) Math.min(count, Math.min(s.remaining(), d.capacity()));
        d.put((ByteBuffer) s.slice().limit(n));
    }

    public static void memSet(long address, int value, long count) {
        ByteBuffer b = byteBuffer(address, (int) count);
        for (int i = 0; i < b.capacity(); i++) {
            b.put(i, (byte) value);
        }
    }

    public static void memSet(ByteBuffer buffer, int value, long count) {
        for (int i = 0; i < Math.min(count, buffer.capacity()); i++) {
            buffer.put(i, (byte) value);
        }
    }

    // ---------------------------------------------------------------- misc

    public static long memAddressSafe(Object o) {
        if (o instanceof ByteBuffer bb) return memAddress(bb);
        if (o instanceof Pointer p) return p.address();
        if (o instanceof Long l) return l;
        return 0L;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void checkBufferOverflow(Object buffer, long count) {
        if (buffer instanceof ByteBuffer bb && count > bb.capacity()) {
            throw new IllegalArgumentException("buffer size mismatch");
        }
    }
}