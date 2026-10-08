package org.lwjgl.stb;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.lwjgl.system.MemoryUtil;

/** {@code STBImage} loader backed by Android's {@link BitmapFactory} (a real decoder). */
public final class STBImage {

    private static volatile String lastError;

    private STBImage() {
    }

    public static String stbi_failure_reason() {
        return lastError;
    }

    public static ByteBuffer stbi_load_from_memory(ByteBuffer bytes, int[] w, int[] h, int[] comp, int req_comp) {
        lastError = null;
        try {
            byte[] data = new byte[bytes.remaining()];
            bytes.duplicate().get(data);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = android.graphics.Bitmap.Config.RGBA_8888;
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            if (bmp == null) {
                lastError = "unable to decode image";
                return null;
            }
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int components = req_comp > 0 ? req_comp : 4;
            if (w != null) w[0] = width;
            if (h != null) h[0] = height;
            if (comp != null) comp[0] = 4;

            int[] pixels = new int[width * height];
            bmp.getPixels(pixels, 0, width, 0, 0, width, height);
            bmp.recycle();

            ByteBuffer out = MemoryUtil.memAlloc(width * height * components);
            for (int pixel : pixels) {
                out.put((byte) ((pixel >> 16) & 0xFF));
                out.put((byte) ((pixel >> 8) & 0xFF));
                out.put((byte) (pixel & 0xFF));
                if (components >= 4) {
                    out.put((byte) ((pixel >> 24) & 0xFF));
                } else if (components == 2) {
                    out.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            out.flip();
            return out;
        } catch (RuntimeException e) {
            lastError = e.getMessage();
            return null;
        }
    }

    public static void nstbi_image_free(long address) {
        if (address != 0L) {
            MemoryUtil.nfree(address);
        }
    }

    public static boolean stbi_is_hdr_from_memory(ByteBuffer buffer) {
        return false;
    }
}