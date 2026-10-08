package org.lwjgl.opengl;

import org.lwjgl.system.MemoryUtil;

/** GL_ARB_debug_output emulation: forwarded as no-ops (GLES has no debug output). */
public final class ARBDebugOutput {

    private ARBDebugOutput() {
    }

    public static void glDebugMessageControlARB(int source, int type, int severity, int[] ids, boolean enabled) {
    }

    public static void glDebugMessageCallbackARB(GLDebugMessageARBCallback callback, long userParam) {
    }

    public static void glDebugMessageInsertARB(int source, int type, int id, int severity, String buf) {
    }

    public static int glGetDebugMessageLogARB(int count, org.lwjgl.PointerBuffer sources, org.lwjgl.PointerBuffer types, org.lwjgl.PointerBuffer ids, org.lwjgl.PointerBuffer severities, org.lwjgl.PointerBuffer lengths, java.nio.ByteBuffer messageLog) {
        return 0;
    }

    public static int glGetDebugMessageLogARB(int count, int[] sources, int[] types, int[] ids, int[] severities, int[] lengths, java.nio.ByteBuffer messageLog) {
        return 0;
    }
}