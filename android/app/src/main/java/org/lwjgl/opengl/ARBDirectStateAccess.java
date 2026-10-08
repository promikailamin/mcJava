package org.lwjgl.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import android.util.Log;

/**
 * GL_ARB_direct_state_access emulation. On GLES there is no DSA; calls are accepted and
 * routed to the closest GLES equivalent where the binding can be inferred, otherwise
 * ignored (the caller's own state tracking applies the resource).
 */
public final class ARBDirectStateAccess {

    private static final String TAG = "DS";

    private ARBDirectStateAccess() {
    }

    public static void glCreateBuffers(int count, int[] buffers) {
        GL33C.glGenBuffers(buffers);
    }

    public static void glCreateBuffers(int n, java.nio.IntBuffer buffers) {
        GL33C.glGenBuffers(buffers);
    }

    public static void glNamedBufferData(int buffer, long size, int usage) {
        Log.w(TAG, "glNamedBufferData " + buffer);
    }

    public static void glNamedBufferData(int buffer, ByteBuffer data, int usage) {
        Log.w(TAG, "glNamedBufferData " + buffer);
    }

    public static void glNamedBufferStorage(int buffer, long size, long data, int flags) {
        Log.w(TAG, "glNamedBufferStorage " + buffer);
    }

    public static void glNamedBufferSubData(int buffer, long offset, ByteBuffer data) {
        Log.w(TAG, "glNamedBufferSubData " + buffer);
    }

    public static void glNamedBufferSubData(int buffer, long offset, long size, long data) {
        Log.w(TAG, "glNamedBufferSubData " + buffer);
    }

    public static void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
    }

    public static long glMapNamedBufferRange(int buffer, long offset, long length, int access) {
        return 0L;
    }

    public static boolean glUnmapNamedBuffer(int buffer) {
        return false;
    }

    public static void glFlushMappedNamedBufferRange(int buffer, long offset, long length) {
    }

    public static void glCreateFramebuffers(int count, int[] framebuffers) {
        GL33C.glGenFramebuffers(framebuffers);
    }

    public static void glNamedFramebufferTexture(int framebuffer, int attachment, int texture, int level) {
        GL33C.glFramebufferTexture2D(0x8D40, attachment, 0x0DE1, texture, level);
    }

    public static void glBlitNamedFramebuffer(int readFramebuffer, int drawFramebuffer, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL33C.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
}