package org.lwjgl.opengl;

@FunctionalInterface
public interface GLDebugMessageCallback {

    void invoke(int source, int type, int id, int severity, int length, long message, long userParam);

    static GLDebugMessageCallback create(GLDebugMessageCallback callback) {
        return callback;
    }

    static String getMessage(int length, long message) {
        if (message == 0L) {
            return "";
        }
        byte[] raw = new byte[Math.max(0, length)];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = org.lwjgl.system.MemoryUtil.memGetByte(message + i);
        }
        return new String(raw, java.nio.charset.StandardCharsets.UTF_8);
    }

    default long address() {
        return 1L;
    }

    default void close() {
    }
}