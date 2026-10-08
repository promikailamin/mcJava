package org.lwjgl.opengl;

import java.nio.ByteBuffer;

/** GL_EXT_debug_label: no-ops on GLES. */
public final class EXTDebugLabel {

    private EXTDebugLabel() {
    }

    public static void glLabelObjectEXT(int type, int object, CharSequence label) {
    }

    public static void glLabelObjectEXT(int type, int object, ByteBuffer label) {
    }

    public static void glGetObjectLabelEXT(int type, int object, int[] bufSize) {
    }
}