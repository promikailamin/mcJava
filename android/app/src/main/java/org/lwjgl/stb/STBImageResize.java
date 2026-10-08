package org.lwjgl.stb;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

/** {@code STBImageResize}: box/bilinear resample in Java over the shim's byte memory. */
public final class STBImageResize {

    private STBImageResize() {
    }

    public static int nstbir_resize_uint8_linear(long src, int srcW, int srcH, int srcStride,
                                                 long dst, int dstW, int dstH, int dstStride,
                                                 int components) {
        if (src == 0L || dst == 0L) {
            return 0;
        }
        ByteBuffer s = MemoryUtil.memByteBuffer(src, srcStride * srcH);
        ByteBuffer d = MemoryUtil.memByteBuffer(dst, (dstStride == 0 ? dstW : dstStride) * dstH);
        int dstRow = dstStride == 0 ? dstW * components : dstStride;

        for (int y = 0; y < dstH; y++) {
            float sy = (srcH == 1) ? 0f : (float) (y * (srcH - 1)) / Math.max(1, dstH - 1);
            int y0 = (int) sy;
            int y1 = Math.min(y0 + 1, srcH - 1);
            float fy = sy - y0;
            for (int x = 0; x < dstW; x++) {
                float sx = (srcW == 1) ? 0f : (float) (x * (srcW - 1)) / Math.max(1, dstW - 1);
                int x0 = (int) sx;
                int x1 = Math.min(x0 + 1, srcW - 1);
                float fx = sx - x0;
                for (int c = 0; c < components; c++) {
                    float a = s.get(y0 * srcStride + x0 * components + c) & 0xFF;
                    float b = s.get(y0 * srcStride + x1 * components + c) & 0xFF;
                    float c0 = s.get(y1 * srcStride + x0 * components + c) & 0xFF;
                    float c1 = s.get(y1 * srcStride + x1 * components + c) & 0xFF;
                    float top = a + (b - a) * fx;
                    float bottom = c0 + (c1 - c0) * fx;
                    float value = top + (bottom - top) * fy;
                    d.put(y * dstRow + x * components + c, (byte) Math.max(0, Math.min(255, (int) value)));
                }
            }
        }
        return 1;
    }
}