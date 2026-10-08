package org.lwjgl.stb;

import java.nio.ByteBuffer;

/** {@code STBImageWrite} surface. PNG is encoded in pure Java. */
public final class STBImageWrite {

    private STBImageWrite() {
    }

    public static int nstbi_write_png_to_func(long write_func, long context, int w, int h, int comp, long data, int stride) {
        STBIWriteCallback callback = STBIWriteCallback.byAddress(write_func);
        if (callback == null) {
            return 0;
        }
        try {
            callback.writePng(w, h, comp, data, stride);
            return 1;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public static int STBI_write_png(String filename, int w, int h, int comp, ByteBuffer data, int stride) {
        return 0;
    }
}