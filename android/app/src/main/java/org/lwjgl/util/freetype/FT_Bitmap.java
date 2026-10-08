package org.lwjgl.util.freetype;

import java.nio.ByteBuffer;

/** Raster bitmap surface. The shim returns empty bitmaps (graceful font fallback). */
public final class FT_Bitmap {

    public long handle;

    public FT_Bitmap() {
    }

    public static FT_Bitmap create(long address) {
        return new FT_Bitmap();
    }

    public int width() {
        return 0;
    }

    public int rows() {
        return 0;
    }

    public int pitch() {
        return 0;
    }

    public int pixel_mode() {
        return FreeType.FT_PIXEL_MODE_GRAY;
    }

    public int num_grays() {
        return 256;
    }

    public ByteBuffer buffer(int size) {
        return ByteBuffer.allocate(Math.max(0, size));
    }

    public long buffer() {
        return 0L;
    }
}