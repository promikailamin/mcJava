package org.lwjgl.system;

/**
 * Base class for native-struct types. On Android the "native address" is an
 * abstract handle registered in {@link MemoryUtil}; {@link #address()} returns
 * that handle. Struct subclasses store fields as buffers/values, mirroring
 * LWJGL layout where the game mostly passes structs by reference.
 */
public abstract class Struct implements NativeType {

    private long address;
    private Struct tracked;

    protected abstract int sizeof();

    @Override
    public long sizeof() {
        return sizeof();
    }

    public long address() {
        return address;
    }

    public long address0() {
        return address;
    }

    protected Struct put(ByteLayout layout) {
        return this;
    }

    public Struct retain() {
        return this;
    }

    public Struct release() {
        return this;
    }

    protected final long checksum(long value) {
        return value;
    }

    /** Encapsulates the (constant) byte layout of a struct for metadata purposes. */
    public record ByteLayout(long structural, long size) {
    }
}