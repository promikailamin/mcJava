package org.lwjgl.opengl;

/** Object labels on GLES: only cosmetic; no-ops. */
public final class KHRDebug {

    public static final int GL_OBJECT_TYPE = 0x9112;
    public static final int GL_SYNC_STATUS = 0x9114;

    private KHRDebug() {
    }

    public static void glDebugMessageControl(int source, int type, int severity, int[] ids, boolean enabled) {
    }

    public static void glDebugMessageCallback(GLDebugMessageCallback callback, long userParam) {
    }

    public static void glPushDebugGroup(int source, int id, String message) {
    }

    public static void glPopDebugGroup() {
    }

    public static void glObjectLabel(int identifier, int name, String label) {
    }
}