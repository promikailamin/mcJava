package org.lwjgl.system;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;

/**
 * Strict forward-American style allocator stack ({@code stackPush}/{@code pop}) backed by
 * individually allocated {@link MemoryUtil} arenas, freed on {@code pop()}.
 */
public class MemoryStack {

    private static final ThreadLocal<MemoryStack> THREAD_STACK = ThreadLocal.withInitial(MemoryStack::new);

    private final java.util.ArrayDeque<Integer> freeStack = new java.util.ArrayDeque<>();
    private int frameBase;

    public static MemoryStack stackGet() {
        return THREAD_STACK.get();
    }

    public static MemoryStack stackPush() {
        MemoryStack stack = stackGet();
        stack.push();
        return stack;
    }

    public static MemoryStack stackGetNoConstraint() {
        return stackGet();
    }

    public static void stackPush(int size, int alignment) {
        stackGet().push(size, alignment);
    }

    public static void stackPop() {
        stackGet().pop();
    }

    public static int getStackSize() {
        return THREAD_STACK.get().frameBase;
    }

    public void push() {
        push(0, 16);
    }

    public void push(int size, int alignment) {
        freeStack.push(frameBase);
        frameBase = 0;
        if (size > 0) {
            nmalloc(size + alignment);
        }
    }

    public void pop() {
        if (freeStack.isEmpty()) {
            throw new IllegalStateException("MemoryStack underflow");
        }
        frameBase = freeStack.pop();
    }

    public void pop(int frame) {
        pop();
    }

    public void free() {
    }

    public long address() {
        return frameBase;
    }

    public int pointer() {
        return frameBase;
    }

    public int getPointer() {
        return frameBase;
    }

    public void putPointer(long pointer) {
        frameBase = (int) pointer;
    }

    public void close() {
        pop();
    }

    // ---------------------------------------------------------------- allocs

    public long nmalloc(long size) {
        return MemoryUtil.nalloc(size);
    }

    public long ncalloc(long size) {
        return MemoryUtil.ncalloc(size, 1);
    }

    public long nrealloc(long address, long size) {
        return MemoryUtil.nrealloc(address, size);
    }

    public long nmallocAligned(long size, int alignment) {
        return MemoryUtil.nalloc(size + alignment);
    }

    public long nalloca(long size) {
        return nmalloc(size);
    }

    public ByteBuffer malloc(int size) {
        return MemoryUtil.memByteBuffer(nmalloc(Math.max(1, size)), Math.max(1, size));
    }

    public ByteBuffer calloc(int size) {
        return MemoryUtil.memCalloc(Math.max(1, size));
    }

    public IntBuffer mallocInt(int count) {
        return MemoryUtil.memAllocInt(Math.max(1, count));
    }

    public IntBuffer callocInt(int count) {
        return MemoryUtil.memCallocInt(Math.max(1, count));
    }

    public FloatBuffer mallocFloat(int count) {
        return MemoryUtil.memByteBuffer(nmalloc((long) count * Float.BYTES), count * Float.BYTES).asFloatBuffer();
    }

    public FloatBuffer callocFloat(int count) {
        FloatBuffer b = mallocFloat(count);
        for (int i = 0; i < b.capacity(); i++) b.put(i, 0f);
        return b;
    }

    public ShortBuffer mallocShort(int count) {
        return MemoryUtil.memByteBuffer(nmalloc((long) count * Short.BYTES), count * Short.BYTES).asShortBuffer();
    }

    public ShortBuffer callocShort(int count) {
        ShortBuffer b = mallocShort(count);
        for (int i = 0; i < b.capacity(); i++) b.put(i, (short) 0);
        return b;
    }

    public LongBuffer mallocLong(int count) {
        return MemoryUtil.memByteBuffer(nmalloc((long) count * Long.BYTES), count * Long.BYTES).asLongBuffer();
    }

    public LongBuffer callocLong(int count) {
        LongBuffer b = mallocLong(count);
        for (int i = 0; i < b.capacity(); i++) b.put(i, 0L);
        return b;
    }

    public DoubleBuffer mallocDouble(int count) {
        return MemoryUtil.memByteBuffer(nmalloc((long) count * Double.BYTES), count * Double.BYTES).asDoubleBuffer();
    }

    public DoubleBuffer callocDouble(int count) {
        DoubleBuffer b = mallocDouble(count);
        for (int i = 0; i < b.capacity(); i++) b.put(i, 0d);
        return b;
    }

    public PointerBuffer mallocPointer(int count) {
        PointerBuffer b = PointerBuffer.allocateDirect(Math.max(1, count));
        return b;
    }

    public PointerBuffer callocPointer(int count) {
        PointerBuffer b = PointerBuffer.calloc(Math.max(1, count));
        return b;
    }

    public ByteBuffer mallocByte(int count) {
        return malloc(count);
    }

    public long UTF8(String text) {
        return UTF8(text, true);
    }

    public long UTF8(String text, boolean nullTerminated) {
        if (text == null) {
            return 0L;
        }
        byte[] data = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long ptr = nmalloc(data.length + (nullTerminated ? 1 : 0));
        MemoryUtil.memPutBytes(ptr, data, 0, data.length);
        if (nullTerminated) {
            MemoryUtil.memPutByte(ptr + data.length, (byte) 0);
        }
        return ptr;
    }

    public long ASCII(String text) {
        return ASCII(text, true);
    }

    public long ASCII(String text, boolean nullTerminated) {
        if (text == null) {
            return 0L;
        }
        byte[] data = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        long ptr = nmalloc(data.length + (nullTerminated ? 1 : 0));
        MemoryUtil.memPutBytes(ptr, data, 0, data.length);
        if (nullTerminated) {
            MemoryUtil.memPutByte(ptr + data.length, (byte) 0);
        }
        return ptr;
    }

    public long nmalloc(int size) {
        return MemoryUtil.nalloc(Math.max(1, size));
    }

    // ---------------------------------------------------------------- puts

    public void nint(long address, int value) {
        MemoryUtil.memPutInt(address, value);
    }

    public void nint(long address, int index, int value) {
        MemoryUtil.memPutInt(address + (long) index * Integer.BYTES, value);
    }

    public void nfloat(long address, float value) {
        MemoryUtil.memPutFloat(address, value);
    }

    public void nfloat(long address, int index, float value) {
        MemoryUtil.memPutFloat(address + (long) index * Float.BYTES, value);
    }

    public void nlong(long address, long value) {
        MemoryUtil.memPutLong(address, value);
    }

    public void nlong(long address, int index, long value) {
        MemoryUtil.memPutLong(address + (long) index * Long.BYTES, value);
    }

    public void nshort(long address, short value) {
        MemoryUtil.memPutShort(address, value);
    }

    public void naddress(long address, long value) {
        MemoryUtil.memPutLong(address, value);
    }

    public void nPointer(long address, long value) {
        MemoryUtil.memPutLong(address, value);
    }
}