package org.lwjgl.opengl;

/** GL 4.3 vertex attribute binding via GLES3.0's equivalent APIs. */
public final class ARBVertexAttribBinding {

    private ARBVertexAttribBinding() {
    }

    public static void glBindVertexBuffer(int bindingindex, int buffer, long offset, int stride) {
        GL33C.glBindVertexBuffer(bindingindex, buffer, offset, stride);
    }

    public static void glVertexAttribBinding(int attribindex, int bindingindex) {
        GL33C.glVertexAttribBinding(attribindex, bindingindex);
    }

    public static void glVertexAttribFormat(int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        GL33C.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
    }

    public static void glVertexAttribIFormat(int attribindex, int size, int type, int relativeoffset) {
        GL33C.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
    }

    public static void glVertexAttribLFormat(int attribindex, int size, int type, int relativeoffset) {
    }

    public static void glVertexBindingDivisor(int bindingindex, int divisor) {
        GL33C.glVertexBindingDivisor(bindingindex, divisor);
    }
}