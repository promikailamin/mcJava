package org.lwjgl.opengl;

import java.nio.ByteBuffer;

/** GL 4.4 buffer storage. GLES3.2 has no glBufferStorage; storage is emulated in Java. */
public final class ARBBufferStorage {

    private ARBBufferStorage() {
    }

    public static void glBufferStorage(int target, long size, long data, int flags) {
    }

    public static void glBufferStorage(int target, ByteBuffer data, int flags) {
    }

    public static void glNamedBufferStorage(int buffer, long size, long data, int flags) {
    }
}