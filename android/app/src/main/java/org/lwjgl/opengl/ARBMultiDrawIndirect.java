package org.lwjgl.opengl;

import java.nio.ByteBuffer;

/** GL 4.3 multi-draw indirect (GLES3.2 supports {@code glMultiDrawElementsIndirect}). */
public final class ARBMultiDrawIndirect {

    private ARBMultiDrawIndirect() {
    }

    public static void glMultiDrawArraysIndirect(int mode, long indirect, int drawcount, int stride) {
        GL33C.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
    }

    public static void glMultiDrawArraysIndirect(int mode, ByteBuffer indirect, int drawcount, int stride) {
        GL33C.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
    }

    public static void glMultiDrawElementsIndirect(int mode, int type, long indirect, int drawcount, int stride) {
        GL33C.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
    }

    public static void glMultiDrawElementsIndirect(int mode, int type, ByteBuffer indirect, int drawcount, int stride) {
        GL33C.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
    }
}