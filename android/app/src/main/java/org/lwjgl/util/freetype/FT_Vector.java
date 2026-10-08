package org.lwjgl.util.freetype;

import org.lwjgl.system.MemoryStack;

/** 26.6 fixed-point 2D vector. */
public final class FT_Vector {

    public long x;
    public long y;

    public static FT_Vector malloc(MemoryStack stack) {
        return new FT_Vector();
    }

    public static FT_Vector create(long address) {
        return new FT_Vector();
    }

    public static FT_Vector malloc() {
        return new FT_Vector();
    }

    public long x() { return x; }
    public long y() { return y; }

    public FT_Vector set(long x, long y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public long sizeof() {
        return 16;
    }

    public long address() {
        return 0L;
    }
}