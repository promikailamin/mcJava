package org.lwjgl.opengl;

@FunctionalInterface
public interface GLDebugMessageARBCallback {

    void invoke(int source, int type, int id, int severity, int length, long message, long userParam);

    static GLDebugMessageARBCallback create(GLDebugMessageARBCallback callback) {
        return callback;
    }

    default long address() {
        return 1L;
    }
}