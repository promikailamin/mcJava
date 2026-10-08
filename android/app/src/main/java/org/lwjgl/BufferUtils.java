package org.lwjgl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/** {@code org.lwjgl.BufferUtils} surface backed by {@link org.lwjgl.system.MemoryUtil} arenas. */
public final class BufferUtils {

    private BufferUtils() {
    }

    public static ByteBuffer createByteBuffer(int size) {
        return org.lwjgl.system.MemoryUtil.memAlloc(size);
    }

    public static ShortBuffer createShortBuffer(int size) {
        return createByteBuffer(size * Short.BYTES).order(java.nio.ByteOrder.nativeOrder()).asShortBuffer();
    }

    public static IntBuffer createIntBuffer(int size) {
        return createByteBuffer(size * Integer.BYTES).order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();
    }

    public static FloatBuffer createFloatBuffer(int size) {
        return createByteBuffer(size * Float.BYTES).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static LongBuffer createLongBuffer(int size) {
        return createByteBuffer(size * Long.BYTES).order(java.nio.ByteOrder.nativeOrder()).asLongBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(int size) {
        return createByteBuffer(size * Double.BYTES).order(java.nio.ByteOrder.nativeOrder()).asDoubleBuffer();
    }

    public static int getElementSizeExponent(java.nio.Buffer buffer) {
        if (buffer instanceof ByteBuffer) return 0;
        if (buffer instanceof ShortBuffer || buffer instanceof CharBuffer) return 1;
        if (buffer instanceof IntBuffer || buffer instanceof FloatBuffer) return 2;
        if (buffer instanceof LongBuffer || buffer instanceof DoubleBuffer) return 3;
        throw new IllegalArgumentException("Unknown buffer type: " + buffer.getClass());
    }

    public static java.nio.ByteBuffer getByteBuffer(long address, int capacity) {
        return org.lwjgl.system.MemoryUtil.memByteBuffer(address, capacity);
    }
}