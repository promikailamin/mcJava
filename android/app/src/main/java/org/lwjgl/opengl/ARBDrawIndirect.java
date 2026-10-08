package org.lwjgl.opengl;

/** GL 4.0 indirect drawing (rendered via GLES3.1 {@code glDrawArraysIndirect}). */
public final class ARBDrawIndirect {

    private ARBDrawIndirect() {
    }

    public static void glDrawArraysIndirect(int mode, long indirect) {
        GL33C.glDrawArraysIndirect(mode, indirect);
    }

    public static void glDrawElementsIndirect(int mode, int type, long indirect) {
        GL33C.glDrawElementsIndirect(mode, type, indirect);
    }
}