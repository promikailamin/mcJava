package org.lwjgl.opengl;

import java.nio.Buffer;

/** GL 4.2 base-instance drawing. Emulated through GL33C (instanced base-vertex). */
public final class ARBBaseInstance {

    private ARBBaseInstance() {
    }

    public static void glDrawArraysInstancedBaseInstance(int mode, int first, int count, int instanceCount, int baseInstance) {
    }

    public static void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices, int instanceCount, int baseVertex, int baseInstance) {
    }

    public static void glDrawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, Buffer indices, int instanceCount, int baseVertex, int baseInstance) {
    }
}