package org.lwjgl.opengl;

/** GL 4.5 clip control. Not supported on GLES3.2; no-op. */
public final class ARBClipControl {

    private ARBClipControl() {
    }

    public static void glClipControl(int origin, int depth) {
    }
}