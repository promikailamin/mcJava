package org.lwjgl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.system.MemoryUtil;

/**
 * Variable-pointer buffer. On Android pointers are <em>address handles</em>
 * ({@link MemoryUtil}), so each slot stores one handle.
 */
public class PointerBuffer implements Comparable<PointerBuffer> {

    private final ByteBuffer storage;
    private final int capacity;
    private int limit;
    private int position;

    private PointerBuffer(ByteBuffer source, int position, int limit) {
        this.storage = source.duplicate().order(ByteOrder.nativeOrder());
        this.capacity = source.capacity() / Long.BYTES;
        this.limit = limit;
        this.position = position;
    }

    public static PointerBuffer allocateDirect(int capacity) {
        ByteBuffer source = MemoryUtil.memAlloc(Math.max(1, capacity) * Long.BYTES);
        return new PointerBuffer(source, 0, capacity);
    }

    public static PointerBuffer allocateDirect(long size) {
        return allocateDirect((int) size);
    }

    public static PointerBuffer calloc(int capacity) {
        PointerBuffer b = allocateDirect(capacity);
        for (int i = 0; i < b.capacity; i++) {
            b.put(i, 0L);
        }
        return b;
    }

    public static PointerBuffer validate(long handle) {
        return null;
    }

    public long address() {
        return storage.position(0) instanceof ByteBuffer ? MemoryUtil.memAddress(storage) : 0L;
    }

    public int capacity() {
        return capacity;
    }

    public int limit() {
        return limit;
    }

    public PointerBuffer limit(int newLimit) {
        this.limit = newLimit;
        return this;
    }

    public int position() {
        return position;
    }

    public PointerBuffer position(int newPosition) {
        this.position = newPosition;
        return this;
    }

    public int remaining() {
        return limit - position;
    }

    public boolean hasRemaining() {
        return remaining() > 0;
    }

    public PointerBuffer clear() {
        position = 0;
        limit = capacity;
        return this;
    }

    public PointerBuffer rewind() {
        position = 0;
        return this;
    }

    public PointerBuffer flip() {
        limit = position;
        position = 0;
        return this;
    }

    public long get() {
        long v = get(position);
        position++;
        return v;
    }

    public long get(int index) {
        return storage.getLong(index * Long.BYTES);
    }

    public PointerBuffer put(long value) {
        put(position, value);
        position++;
        return this;
    }

    public PointerBuffer put(int index, long value) {
        storage.putLong(index * Long.BYTES, value);
        return this;
    }

    public PointerBuffer put(PointerBuffer src) {
        while (src.hasRemaining()) {
            put(src.get());
        }
        return this;
    }

    public void free() {
        MemoryUtil.memFree(storage);
    }

    @Override
    public int compareTo(PointerBuffer other) {
        return Long.compare(get(0), other.get(0));
    }
}