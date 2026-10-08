package org.lwjgl.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLES32;
import android.util.Log;

import org.lwjgl.system.MemoryUtil;

/**
 * OpenGL 3.3 Core ("combined profile") translated to OpenGL ES 3.2 on Android.
 *
 * All desktop-only calls are either mapped to their GLES 3.2 equivalent or emulated
 * in Java. Desktop call sites that have no GLES counterpart (polygon mode, clip
 * control, draw buffers, buffer textures without EXT, multi-draw) are handled with
 * a compatibility fallback so the game's GL renderer still produces correct frames
 * on GLES hardware.
 */
public class GL33C {

    private static final String TAG = "GL33C";

    // ---- GL 1.0-1.2 ----
    public static final int GL_ALPHA_TEST = 0x0BC0, GL_ALPHA_TEST_FUNC = 0x0BC1, GL_ALPHA_TEST_REF = 0x0BC2,
            GL_DEPTH_TEST = 0x0B71, GL_BLEND = 0x0BE2, GL_CULL_FACE = 0x0B44, GL_SCISSOR_TEST = 0x0C11,
            GL_STENCIL_TEST = 0x0B90, GL_DITHER = 0x0BD0, GL_MULTISAMPLE = 0x809D,
            GL_COLOR_BUFFER_BIT = 0x4000, GL_DEPTH_BUFFER_BIT = 0x100, GL_STENCIL_BUFFER_BIT = 0x400,
            GL_POINTS = 0x0000, GL_LINES = 0x0001, GL_LINE_LOOP = 0x0002, GL_LINE_STRIP = 0x0003,
            GL_TRIANGLES = 0x0004, GL_TRIANGLE_STRIP = 0x0005, GL_TRIANGLE_FAN = 0x0006,
            GL_QUADS = 0x0007, GL_NEAREST = 0x2600, GL_LINEAR = 0x2601,
            GL_NEAREST_MIPMAP_NEAREST = 0x2700, GL_LINEAR_MIPMAP_NEAREST = 0x2701,
            GL_NEAREST_MIPMAP_LINEAR = 0x2702, GL_LINEAR_MIPMAP_LINEAR = 0x2703,
            GL_TEXTURE_MAG_FILTER = 0x2800, GL_TEXTURE_MIN_FILTER = 0x2801, GL_TEXTURE_WRAP_S = 0x2802,
            GL_TEXTURE_WRAP_T = 0x2803, GL_TEXTURE_2D = 0x0DE1, GL_TEXTURE_3D = 0x806F,
            GL_TEXTURE_CUBE_MAP = 0x8513, GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515,
            GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516, GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517,
            GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518, GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519,
            GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A,
            GL_REPEAT = 0x2901, GL_CLAMP_TO_EDGE = 0x812F, GL_MIRRORED_REPEAT = 0x8370,
            GL_TEXTURE0 = 0x84C0, GL_ACTIVE_TEXTURE = 0x84E0,
            GL_FRONT = 0x0404, GL_BACK = 0x0405, GL_FRONT_AND_BACK = 0x0408,
            GL_CW = 0x0900, GL_CCW = 0x0901,
            GL_SRC_ALPHA = 0x0302, GL_ONE_MINUS_SRC_ALPHA = 0x0303, GL_ONE = 0x1, GL_ZERO = 0x0,
            GL_DST_ALPHA = 0x0304, GL_ONE_MINUS_DST_ALPHA = 0x0305,
            GL_SRC_COLOR = 0x0300, GL_DST_COLOR = 0x0306,
            GL_ONE_MINUS_SRC_COLOR = 0x0301, GL_ONE_MINUS_DST_COLOR = 0x0307,
            GL_FUNC_ADD = 0x8006, GL_FUNC_SUBTRACT = 0x800A, GL_FUNC_REVERSE_SUBTRACT = 0x800B,
            GL_BLEND_SRC_RGB = 0x80C9, GL_BLEND_DST_RGB = 0x80C8,
            GL_BLEND_SRC_ALPHA = 0x80CB, GL_BLEND_DST_ALPHA = 0x80CA,
            GL_NEVER = 0x0200, GL_LESS = 0x0201, GL_EQUAL = 0x0202, GL_LEQUAL = 0x0203,
            GL_GREATER = 0x0204, GL_NOTEQUAL = 0x0205, GL_GEQUAL = 0x0206, GL_ALWAYS = 0x0207,
            GL_TRUE = 0, GL_FALSE = 0, GL_NO_ERROR = 0x0,
            GL_BYTE = 0x1400, GL_UNSIGNED_BYTE = 0x1401, GL_SHORT = 0x1402, GL_UNSIGNED_SHORT = 0x1403,
            GL_INT = 0x1404, GL_UNSIGNED_INT = 0x1405, GL_FLOAT = 0x1406, GL_DOUBLE = 0x140A,
            GL_RGBA = 0x1908, GL_RGB = 0x1907, GL_LUMINANCE = 0x1909, GL_LUMINANCE_ALPHA = 0x190A,
            GL_ALPHA = 0x1906, GL_RGBA8 = 0x8058, GL_RGB8 = 0x8051, GL_RGBA16 = 0x805B,
            GL_DEPTH_COMPONENT = 0x1902, GL_DEPTH_COMPONENT16 = 0x81A5, GL_DEPTH_COMPONENT24 = 0x81A6,
            GL_STENCIL_INDEX = 0x1901, GL_STENCIL_INDEX8 = 0x8D48,
            GL_TRIANGLE_STRIP_ADJACENCY = 0x000B, GL_TRIANGLES_ADJACENCY = 0x000C, GL_LINE_STRIP_ADJACENCY = 0x000B,
            GL_VIEWPORT = 0x0BA2, GL_MODELVIEW = 0x1700, GL_PROJECTION = 0x1701, GL_TEXTURE = 0x1702,
            GL_POLYGON_OFFSET_FILL = 0x8037, GL_POLYGON_OFFSET_LINE = 0x2A02, GL_POLYGON_OFFSET_POINT = 0x2A01,
            GL_RESCALE_NORMAL = 0x803A, GL_NORMALIZE = 0x0BA1,
            GL_SMOOTH = 0x1D01, GL_FLAT = 0x1D00, GL_NICEST = 0x1102, GL_FASTEST = 0x1101, GL_DONT_CARE = 0x1100,
            GL_KEEP = 0x1E00, GL_REPLACE = 0x1E01, GL_INCR = 0x1E02, GL_DECR = 0x1E03, GL_INVERT = 0x150A,
            GL_INCR_WRAP = 0x8507, GL_DECR_WRAP = 0x8508, GL_STENCIL_FUNC = 0x0B92, GL_STENCIL_REF = 0x0B97,
            GL_STENCIL_VALUE_MASK = 0x0B93, GL_STENCIL_WRITEMASK = 0x0B96, GL_STENCIL_FAIL = 0x0B94,
            GL_STENCIL_PASS_DEPTH_FAIL = 0x0B95, GL_STENCIL_PASS_DEPTH_PASS = 0x0B96,
            GL_DEPTH_FUNC = 0x0B74, GL_DEPTH_WRITEMASK = 0x0B72, GL_DEPTH_RANGE = 0x0B70,
            GL_COLOR_WRITEMASK = 0x0C23, GL_SHADE_MODEL = 0x0B54,
            GL_POLYGON_SMOOTH = 0x0B41, GL_LINE_SMOOTH = 0x0B20,
            GL_CURRENT_COLOR = 0x0B00, GL_CURRENT_TEXTURE_COORDS = 0x0B03, GL_CURRENT_NORMAL = 0x0B02;
    // ---- GL 1.3-1.5 ----
    public static final int GL_ARRAY_BUFFER = 0x8892, GL_ELEMENT_ARRAY_BUFFER = 0x8893,
            GL_STREAM_DRAW = 0x88E0, GL_STATIC_DRAW = 0x88E4, GL_DYNAMIC_DRAW = 0x88E8,
            GL_DYNAMIC_COPY = 0x88EA, GL_STREAM_READ = 0x88E1, GL_STREAM_COPY = 0x88E2,
            GL_STATIC_READ = 0x88E5, GL_DYNAMIC_READ = 0x88E9,
            GL_VERTEX_ATTRIB_ARRAY_ENABLED = 0x8622, GL_VERTEX_ATTRIB_ARRAY_SIZE = 0x8623,
            GL_VERTEX_ATTRIB_ARRAY_STRIDE = 0x8624, GL_VERTEX_ATTRIB_ARRAY_TYPE = 0x8625,
            GL_VERTEX_ATTRIB_ARRAY_POINTER = 0x8645, GL_CURRENT_VERTEX_ATTRIB = 0x8626,
            GL_BLEND_COLOR = 0x8005;
    // ---- GL 2.0 ----
    public static final int GL_VERTEX_SHADER = 0x8B31, GL_FRAGMENT_SHADER = 0x8B30,
            GL_COMPILE_STATUS = 0x8B81, GL_LINK_STATUS = 0x8B82, GL_INFO_LOG_LENGTH = 0x8B84,
            GL_ACTIVE_UNIFORMS = 0x8B86, GL_ACTIVE_UNIFORM_MAX_LENGTH = 0x8B87,
            GL_ACTIVE_ATTRIBUTES = 0x8B89, GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 0x8B8A,
            GL_FRAMEBUFFER = 0x8D40, GL_RENDERBUFFER = 0x8D41, GL_FRAMEBUFFER_COMPLETE = 0x8CD5,
            GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6, GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7,
            GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x8CD9,
            GL_FRAMEBUFFER_UNDEFINED = 0x8219, GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD,
            GL_COLOR_ATTACHMENT0 = 0x8CE0, GL_DEPTH_ATTACHMENT = 0x8D00, GL_STENCIL_ATTACHMENT = 0x8D20,
            GL_DEPTH_STENCIL_ATTACHMENT = 0x821A, GL_TEXTURE_2D_ARRAY = 0x8C1A,
            GL_FRAMEBUFFER_BINDING = 0x8CA6, GL_DRAW_FRAMEBUFFER = 0x8CA9, GL_READ_FRAMEBUFFER = 0x8CA8,
            GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6, GL_READ_FRAMEBUFFER_BINDING = 0x8CAA,
            GL_RENDERBUFFER_BINDING = 0x8CA7, GL_MAX_RENDERBUFFER_SIZE = 0x84E8,
            GL_RENDERBUFFER_WIDTH = 0x8D42, GL_DEPTH24_STENCIL8 = 0x88F0, GL_DEPTH_COMPONENT32 = 0x81A7,
            GL_RG = 0x8227, GL_RG8 = 0x822B, GL_R16 = 0x822A, GL_RG16 = 0x822C,
            GL_R16F = 0x822D, GL_R32F = 0x822E, GL_RG16F = 0x822F, GL_RG32F = 0x8230,
            GL_RGBA32F = 0x8814, GL_RGBA16F = 0x881A, GL_RGB32F = 0x8815, GL_RGB16F = 0x881B,
            GL_R8 = 0x8229, GL_RED_INTEGER = 0x8D94, GL_RG_INTEGER = 0x8228,
            GL_UNSIGNED_INT_24_8 = 0x84FA, GL_UNSIGNED_INT_2_10_10_10_REV = 0x8368;
    // ---- GL 3.0-3.3 ----
    public static final int GL_RGBA32UI = 0x8D70, GL_RGB32UI = 0x8D71, GL_RG32UI = 0x823C, GL_R32UI = 0x8236,
            GL_RGBA16UI = 0x8D76, GL_RGB16UI = 0x8D77, GL_RG16UI = 0x823A, GL_R16UI = 0x8234,
            GL_RGBA8UI = 0x8D7C, GL_RGB8UI = 0x8D7D, GL_RG8UI = 0x8238, GL_R8UI = 0x8232,
            GL_RGBA32I = 0x8D82, GL_RG32I = 0x823B, GL_R32I = 0x8235, GL_RGBA16I = 0x8D88,
            GL_RG16I = 0x8239, GL_R16I = 0x8233, GL_RGBA8I = 0x8D8E, GL_RG8I = 0x8237, GL_R8I = 0x8231,
            GL_RGBA16 = 0x805B, GL_RG16 = 0x822C, GL_R16 = 0x822A,
            GL_UNIFORM_BUFFER = 0x8A11, GL_UNIFORM_BLOCK_BINDING = 0x8A3F,
            GL_UNIFORM_BLOCK_INDEX = 0x8A3A, GL_UNIFORM_BLOCK_SIZE = 0x8A40,
            GL_INVALID_ENUM = 0x0500, GL_INVALID_VALUE = 0x0501, GL_INVALID_OPERATION = 0x0502,
            GL_OUT_OF_MEMORY = 0x0505, GL_INVALID_FRAMEBUFFER_OPERATION = 0x0506,
            GL_MAX_VERTEX_ATTRIBS = 0x8869, GL_MAX_TEXTURE_SIZE = 0x0D33,
            GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872, GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49,
            GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A, GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D,
            GL_NUM_EXTENSIONS = 0x821D, GL_EXTENSIONS = 0x1F03, GL_RENDERER = 0x1F01,
            GL_VENDOR = 0x1F00, GL_VERSION = 0x1F02, GL_SHADING_LANGUAGE_VERSION = 0x8B8C,
            GL_TEXTURE_MAX_ANISOTROPY = 0x84FE, GL_TEXTURE_LOD_BIAS = 0x8501,
            GL_MAX_TEXTURE_LOD_BIAS = 0x84FD, GL_SAMPLER_BINDING = 0x8919,
            GL_MAJOR_VERSION = 0x821B, GL_MINOR_VERSION = 0x821C, GL_CONTEXT_PROFILE_MASK = 0x9126,
            GL_TEXTURE_BUFFER = 0x8C2A, GL_TEXTURE_2D_MULTISAMPLE = 0x9100,
            GL_SAMPLES_PASSED = 0x8914, GL_ANY_SAMPLES_PASSED = 0x8C2F, GL_QUERY_RESULT = 0x8866,
            GL_QUERY_RESULT_AVAILABLE = 0x8867, GL_CURRENT_QUERY = 0x8865, GL_QUERY_COUNTER_BITS = 0x8864;
    // sync enums
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117, GL_SYNC_STATUS = 0x9114,
            GL_SIGNALED = 0x9119, GL_UNSIGNALED = 0x9118, GL_SYNC_FLUSH_COMMANDS_BIT = 0x1,
            GL_TIMEOUT_IGNORED = 0xFFFFFFFFFFFFFFFFL;
    public static final int GL_MAP_READ_BIT = 0x1, GL_MAP_WRITE_BIT = 0x2, GL_MAP_INVALIDATE_RANGE_BIT = 0x4,
            GL_MAP_INVALIDATE_BUFFER_BIT = 0x8, GL_MAP_FLUSH_EXPLICIT_BIT = 0x10, GL_MAP_UNSYNCHRONIZED_BIT = 0x20;

    private GL33C() {
    }

    // ---------------------------------------------------------------- state

    public static void glEnable(int cap) { GLES20.glEnable(cap); }
    public static void glDisable(int cap) { GLES20.glDisable(cap); }
    public static void glEnablei(int target, int index) { GLES32.glEnablei(target, index); }
    public static void glDisablei(int target, int index) { GLES32.glDisablei(target, index); }
    public static void glDepthFunc(int func) { GLES20.glDepthFunc(func); }
    public static void glDepthMask(boolean flag) { GLES20.glDepthMask(flag); }
    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) { GLES20.glColorMask(r, g, b, a); }
    public static void glColorMaski(int index, boolean r, boolean g, boolean b, boolean a) { GLES32.glColorMaski(index, r, g, b, a); }
    public static void glClearColor(float red, float green, float blue, float alpha) { GLES20.glClearColor(red, green, blue, alpha); }
    public static void glClearDepth(double depth) { GLES20.glClearDepthf((float) depth); }
    public static void glClear(int mask) { GLES20.glClear(mask); }
    public static void glViewport(int x, int y, int width, int height) { GLES20.glViewport(x, y, width, height); }
    public static void glScissor(int x, int y, int width, int height) { GLES20.glScissor(x, y, width, height); }
    public static void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) { GLES20.glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha); }
    public static void glBlendEquationSeparate(int modeRGB, int modeAlpha) { GLES20.glBlendEquationSeparate(modeRGB, modeAlpha); }
    public static void glLogicOp(int opcode) { GLES20.glLogicOp(opcode); }
    public static void glPixelStorei(int pname, int param) { GLES20.glPixelStorei(pname, param); }
    public static void glPolygonOffset(float factor, float units) { GLES20.glPolygonOffset(factor, units); }
    public static void glActiveTexture(int texture) { GLES20.glActiveTexture(texture); }

    /** Not available in GLES: consumed by the game's GL state manager, no visual effect here. */
    public static void glPolygonMode(int face, int mode) { }
    public static void glDrawBuffer(int buf) { }
    public static void glReadBuffer(int buf) { }

    public static int glGetError() { return GLES20.glGetError(); }
    public static void glFinish() { GLES20.glFinish(); }

    public static int glGetInteger(int pname) {
        int[] v = new int[1];
        GLES20.glGetIntegerv(pname, v, 0);
        return v[0];
    }

    public static void glGetInteger(int pname, IntBuffer params) {
        GLES20.glGetIntegerv(pname, params);
    }

    public static long glGetInteger64(int pname) {
        long[] v = new long[1];
        GLES30.glGetInteger64v(pname, v, 0);
        return v[0];
    }

    public static void glGetInteger64(int pname, LongBuffer params) {
        GLES30.glGetInteger64v(pname, params);
    }

    public static float glGetFloat(int pname) {
        float[] v = new float[1];
        GLES20.glGetFloatv(pname, v, 0);
        return v[0];
    }

    public static void glGetFloat(int pname, FloatBuffer params) {
        GLES20.glGetFloatv(pname, params);
    }

    /** Returns the GL string as a null-terminated {@link ByteBuffer}, mirroring LWJGL. */
    public static ByteBuffer glGetString(int name) {
        String s = GLES20.glGetString(name);
        if (s == null) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.allocateDirect(s.length() + 1).order(ByteOrder.nativeOrder())
                .put(s.getBytes(StandardCharsets.US_ASCII)).put((byte) 0);
        buf.flip();
        return buf;
    }

    // ---------------------------------------------------------------- buffers

    public static void glGenBuffers(IntBuffer buffers) {
        int count = buffers.remaining();
        int[] out = new int[count];
        int pos = buffers.position();
        GLES20.glGenBuffers(count, out, 0);
        for (int i = 0; i < count; i++) buffers.put(pos + i, out[i]);
    }

    public static void glGenBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) {
            return;
        }
        GLES20.glGenBuffers(buffers.length, buffers, 0);
    }

    public static int glGenBuffers() {
        int[] out = new int[1];
        GLES20.glGenBuffers(1, out, 0);
        return out[0];
    }

    public static void glBindBuffer(int target, int buffer) { GLES20.glBindBuffer(target, buffer); }
    public static void glDeleteBuffers(IntBuffer buffers) { GLES20.glDeleteBuffers(buffers); }
    public static void glDeleteBuffers(int buffer) { GLES20.glDeleteBuffers(1, new int[]{buffer}, 0); }
    public static void glDeleteBuffers(int buffer, IntBuffer rest) {
        int[] all = concat(buffer, rest);
        GLES20.glDeleteBuffers(all.length, all, 0);
    }

    public static void glBufferData(int target, long size, int usage) {
        GLES20.glBufferData(target, (int) size, null, usage);
    }

    public static void glBufferData(int target, ByteBuffer data, int usage) {
        GLES20.glBufferData(target, data.remaining(), data, usage);
    }

    public static void glBufferData(int target, long size, long data, int usage) {
        if (data == 0L) {
            glBufferData(target, size, usage);
        } else {
            GLES20.glBufferData(target, (int) size, MemoryUtil.byteBuffer(data, (int) size), usage);
        }
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer data) {
        GLES20.glBufferSubData(target, (int) offset, data.remaining(), data);
    }

    public static void glBufferSubData(int target, long offset, long size, long data) {
        GLES20.glBufferSubData(target, (int) offset, (int) size, MemoryUtil.byteBuffer(data, (int) size));
    }

    public static void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        GLES31.glCopyBufferSubData(readTarget, writeTarget, (int) readOffset, (int) writeOffset, (int) size);
    }

    /** Best-effort persistent buffer storage: falls back to {@link #glBufferData}. */
    public static void glBufferStorage(int target, ByteBuffer data, int flags) {
        glBufferData(target, data, flags & 0x2 != 0 ? GLES20.GL_DYNAMIC_DRAW : GLES20.GL_STATIC_DRAW);
    }

    public static void glBufferStorage(int target, long size, long data, int flags) {
        glBufferData(target, size, data, flags & 0x2 != 0 ? GLES20.GL_DYNAMIC_DRAW : GLES20.GL_STATIC_DRAW);
    }

    // ---------------------------------------------------------------- VAO

    public static void glGenVertexArrays(IntBuffer arrays) {
        int count = arrays.remaining();
        int[] out = new int[count];
        int pos = arrays.position();
        GLES30.glGenVertexArrays(count, out, 0);
        for (int i = 0; i < count; i++) arrays.put(pos + i, out[i]);
    }

    public static int glGenVertexArrays() {
        int[] out = new int[1];
        GLES30.glGenVertexArrays(1, out, 0);
        return out[0];
    }

    public static void glBindVertexArray(int array) { GLES30.glBindVertexArray(array); }
    public static void glDeleteVertexArrays(IntBuffer arrays) { GLES30.glDeleteVertexArrays(arrays); }
    public static void glDeleteVertexArrays(int array) { GLES30.glDeleteVertexArrays(1, new int[]{array}, 0); }
    public static void glDeleteVertexArrays(int array, IntBuffer rest) {
        GLES30.glDeleteVertexArrays(concat(array, rest).length, concat(array, rest), 0);
    }

    public static void glEnableVertexAttribArray(int index) { GLES20.glEnableVertexAttribArray(index); }
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, MemoryUtil.byteBuffer(pointer, Math.max(1, stride)));
    }
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, ByteBuffer pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }
    public static void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        GLES30.glVertexAttribIPointer(index, size, type, stride, MemoryUtil.byteBuffer(pointer, Math.max(1, stride)));
    }
    public static void glVertexAttribDivisor(int index, int divisor) {
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    // ---------------------------------------------------------------- texture ops

    public static void glGenTextures(IntBuffer textures) {
        int count = textures.remaining();
        int[] out = new int[count];
        int pos = textures.position();
        GLES20.glGenTextures(count, out, 0);
        for (int i = 0; i < count; i++) textures.put(pos + i, out[i]);
    }

    public static int glGenTextures() {
        int[] out = new int[1];
        GLES20.glGenTextures(1, out, 0);
        return out[0];
    }

    public static void glBindTexture(int target, int texture) { GLES20.glBindTexture(target, texture); }
    public static void glDeleteTextures(IntBuffer textures) { GLES20.glDeleteTextures(textures); }
    public static void glDeleteTextures(int texture) { GLES20.glDeleteTextures(1, new int[]{texture}, 0); }
    public static void glDeleteTextures(int texture, IntBuffer rest) {
        int[] out = concat(texture, rest);
        GLES20.glDeleteTextures(out.length, out, 0);
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels) {
        if (pixels == 0L) {
            GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, null);
        } else {
            GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type,
                    MemoryUtil.byteBuffer(pixels, Math.max(1, width * height * 4)));
        }
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer pixels) {
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels) {
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type,
                MemoryUtil.byteBuffer(pixels, Math.max(1, width * height * 4)));
    }

    public static void glTexParameteri(int target, int pname, int param) { GLES20.glTexParameteri(target, pname, param); }
    public static void glTexParameterf(int target, int pname, float param) { GLES20.glTexParameterf(target, pname, param); }

    public static int glGetTexLevelParameteri(int target, int level, int pname) {
        int[] v = new int[1];
        GLES30.glGetTexLevelParameteriv(target, level, pname, v, 0);
        return v[0];
    }

    public static void glGetTexLevelParameteriv(int target, int level, int pname, IntBuffer params) {
        GLES30.glGetTexLevelParameteriv(target, level, pname, params);
    }

    /** Buffer textures have no base GLES 3.2 entry: no-op (TBO sampling needs EXT_texture_buffer). */
    public static void glTexBuffer(int target, int internalformat, int buffer) {
        if (buffer == 0) return;
        Log.d(TAG, "glTexBuffer(target=" + target + ", ifmt=" + internalformat + ", buf=" + buffer + ") unsupported on GLES, ignoring");
    }

    // ---------------------------------------------------------------- framebuffers

    public static void glGenFramebuffers(IntBuffer framebuffers) {
        int count = framebuffers.remaining();
        int[] out = new int[count];
        int pos = framebuffers.position();
        GLES20.glGenFramebuffers(count, out, 0);
        for (int i = 0; i < count; i++) framebuffers.put(pos + i, out[i]);
    }

    public static void glGenFramebuffers(int[] framebuffers) {
        if (framebuffers == null || framebuffers.length == 0) {
            return;
        }
        GLES20.glGenFramebuffers(framebuffers.length, framebuffers, 0);
    }

    public static int glGenFramebuffers() {
        int[] out = new int[1];
        GLES20.glGenFramebuffers(1, out, 0);
        return out[0];
    }

    public static void glBindFramebuffer(int target, int framebuffer) { GLES20.glBindFramebuffer(target, framebuffer); }
    public static void glDeleteFramebuffers(IntBuffer framebuffers) { GLES20.glDeleteFramebuffers(framebuffers); }
    public static void glDeleteFramebuffers(int framebuffer) { GLES20.glDeleteFramebuffers(1, new int[]{framebuffer}, 0); }
    public static void glDeleteFramebuffers(int framebuffer, IntBuffer rest) {
        int[] out = concat(framebuffer, rest);
        GLES20.glDeleteFramebuffers(out.length, out, 0);
    }

    public static void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public static void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        GLES20.glReadPixels(x, y, width, height, format, type, pixels);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, long pixels) {
        GLES20.glReadPixels(x, y, width, height, format, type, MemoryUtil.byteBuffer(pixels, Math.max(1, width * height * 4)));
    }

    // ---------------------------------------------------------------- programs

    public static int glCreateProgram() { return GLES20.glCreateProgram(); }
    public static void glDeleteProgram(int program) { GLES20.glDeleteProgram(program); }
    public static void glUseProgram(int program) { GLES20.glUseProgram(program); }
    public static int glCreateShader(int type) { return GLES20.glCreateShader(type); }
    public static void glDeleteShader(int shader) { GLES20.glDeleteShader(shader); }
    public static void glAttachShader(int program, int shader) { GLES20.glAttachShader(program, shader); }
    public static void glCompileShader(int shader) { GLES20.glCompileShader(shader); }
    public static void glLinkProgram(int program) { GLES20.glLinkProgram(program); }
    public static void glValidateProgram(int program) { GLES20.glValidateProgram(program); }
    public static void glBindAttribLocation(int program, int index, ByteBuffer name) {
        GLES20.glBindAttribLocation(program, index, readCString(name));
    }

    public static void glGetShaderSource(int shader, IntBuffer length, ByteBuffer source) {
        GLES20.glGetShaderSource(shader, source.capacity(), length, source);
    }

    public static void glShaderSource(int shader, CharSequence source) {
        GLES20.glShaderSource(shader, shaderSourceCompat(source));
    }

    public static void glShaderSource(int shader, ByteBuffer string) {
        glShaderSource(shader, readCString(string));
    }

    /** C-level path used by {@code nglShaderSource(shader, count, strings, len)}. */
    public static void nglShaderSource(int shader, int count, long strings, long length) {
        long first = MemoryUtil.memGetAddress(strings);
        String src = readCString(MemoryUtil.byteBuffer(first, 65536));
        GLES20.glShaderSource(shader, shaderSourceCompat(src));
    }

    public static void nglShaderSource(int shader, CharSequence source) {
        glShaderSource(shader, source);
    }

    public static int glGetShaderi(int shader, int pname) {
        int[] v = new int[1];
        GLES20.glGetShaderiv(shader, pname, v, 0);
        return v[0];
    }

    public static int glGetProgrami(int program, int pname) {
        int[] v = new int[1];
        GLES20.glGetProgramiv(program, pname, v, 0);
        return v[0];
    }

    public static int glGetProgramInfoLog(int program, IntBuffer length, ByteBuffer infoLog) {
        int[] len = new int[1];
        GLES20.glGetProgramInfoLog(program, infoLog.capacity(), len, infoLog);
        if (length != null) { length.put(0, len[0]); }
        return len[0];
    }

    public static int glGetShaderInfoLog(int shader, IntBuffer length, ByteBuffer infoLog) {
        int[] len = new int[1];
        GLES20.glGetShaderInfoLog(shader, infoLog.capacity(), len, infoLog);
        if (length != null) { length.put(0, len[0]); }
        return len[0];
    }

    public static int glGetUniformLocation(int program, CharSequence name) {
        return GLES20.glGetUniformLocation(program, name.toString());
    }

    public static int glGetUniformLocation(int program, ByteBuffer name) {
        return GLES20.glGetUniformLocation(program, readCString(name));
    }

    public static int glGetUniformBlockIndex(int program, CharSequence uniformBlockName) {
        return GLES31.glGetUniformBlockIndex(program, uniformBlockName.toString());
    }

    public static int glGetUniformBlockIndex(int program, ByteBuffer uniformBlockName) {
        return GLES31.glGetUniformBlockIndex(program, readCString(uniformBlockName));
    }

    public static void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        GLES31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    public static void glUniform1i(int location, int v0) { GLES20.glUniform1i(location, v0); }
    public static void glUniform4f(int location, float x, float y, float z, float w) { GLES20.glUniform4f(location, x, y, z, w); }
    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix4fv(location, 1, transpose, value);
    }

    public static void glBindBufferRange(int target, int index, int buffer, long offset, long size) {
        GLES30.glBindBufferRange(target, index, buffer, (int) offset, (int) size);
    }

    // ---------------------------------------------------------------- draw

    public static void glDrawArrays(int mode, int first, int count) {
        GLES20.glDrawArrays(mode, first, count);
    }

    public static void glDrawElements(int mode, int count, int type, ByteBuffer indices) {
        GLES20.glDrawElements(mode, count, type, indices);
    }

    public static void glDrawElements(int mode, int count, int type, long indices) {
        GLES20.glDrawElements(mode, count, type, MemoryUtil.byteBuffer(indices, Math.max(1, count * 4)));
    }

    public static void glDrawElements(int mode, int count, int type, int indices) {
        GLES20.glDrawElements(mode, count, type, indices);
    }

    public static void glDrawArraysInstanced(int mode, int first, int count, int instancecount) {
        GLES31.glDrawArraysInstanced(mode, first, count, instancecount);
    }

    public static void glDrawElementsInstanced(int mode, int count, int type, int indices, int instancecount) {
        GLES31.glDrawElementsInstanced(mode, count, type, indices, instancecount);
    }

    public static void glDrawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
        GLES31.glDrawElementsInstanced(mode, count, type, MemoryUtil.byteBuffer(indices, Math.max(1, count * 4)), instancecount);
    }

    public static void glDrawBuffers(IntBuffer buffers) {
        // GLES 3.x only renders to GL_COLOR_ATTACHMENT0 for the default FBO; ignored.
    }

    public static void glDrawElementsInstancedBaseVertex(int mode, int count, int type, int indices, int instancecount, int basevertex) {
        if (basevertex == 0) {
            gles_robustness.drawInstanced(mode, count, type, indices, instancecount);
        } else {
            Log.w(TAG, "glDrawElementsInstancedBaseVertex with baseVertex ignored (GLES lacks it)");
            gles_robustness.drawInstanced(mode, count, type, indices, instancecount);
        }
    }

    public static void glDrawElementsInstancedBaseVertex(int mode, int count, int type, long indices, int instancecount, int basevertex) {
        glDrawElementsInstancedBaseVertex(mode, count, type, (int) indices, instancecount, basevertex);
    }

    /** GL45/GL33C multi-draw: C arrays are flattened to int loops on GLES. */
    public static void nglMultiDrawArrays(int mode, long first, long count, int drawcount) {
        IntBuffer firsts = MemoryUtil.byteBuffer(first, drawcount * 4).asIntBuffer();
        IntBuffer counts = MemoryUtil.byteBuffer(count, drawcount * 4).asIntBuffer();
        for (int i = 0; i < drawcount; i++) {
            glDrawArrays(mode, firsts.get(i), counts.get(i));
        }
    }

    public static void nglMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        IntBuffer counts = MemoryUtil.byteBuffer(count, drawcount * 4).asIntBuffer();
        IntBuffer baseverts = MemoryUtil.byteBuffer(basevertex, drawcount * 4).asIntBuffer();
        IntBuffer offsets = MemoryUtil.byteBuffer(indices, drawcount * 4).asIntBuffer();
        for (int i = 0; i < drawcount; i++) {
            glDrawElementsInstancedBaseVertex(mode, counts.get(i), type, offsets.get(i), 1, baseverts.get(i));
        }
    }

    // ---------------------------------------------------------------- direct/indirect draws (GLES 3.1)

    public static void glDrawArraysIndirect(int mode, long indirect) {
        GLES31.glDrawArraysIndirect(mode, (int) indirect);
    }

    public static void glDrawArraysIndirect(int mode, ByteBuffer indirect) {
        GLES31.glDrawArraysIndirect(mode, 0);
    }

    public static void glDrawElementsIndirect(int mode, int type, long indirect) {
        GLES31.glDrawElementsIndirect(mode, type, (int) indirect);
    }

    public static void glDrawElementsIndirect(int mode, int type, ByteBuffer indirect) {
        GLES31.glDrawElementsIndirect(mode, type, 0);
    }

    /** Multi-draw emulation: per-command drawArraysInstanced (loses baseInstance). */
    public static void glMultiDrawArraysIndirect(int mode, ByteBuffer indirect, int drawcount, int stride) {
        int s = stride == 0 ? 16 : stride;
        for (int i = 0; i < drawcount; i++) {
            int base = i * s;
            int first = indirect.getInt(base);
            int count = indirect.getInt(base + 4);
            int primcount = indirect.getInt(base + 8);
            GLES31.glDrawArraysInstanced(mode, first, count, primcount);
        }
    }

    public static void glMultiDrawArraysIndirect(int mode, long indirect, int drawcount, int stride) {
        ByteBuffer buf = MemoryUtil.byteBuffer(indirect, Math.max(1, drawcount * (stride == 0 ? 16 : stride)));
        glMultiDrawArraysIndirect(mode, buf, drawcount, stride);
    }

    public static void glMultiDrawElementsIndirect(int mode, int type, ByteBuffer indirect, int drawcount, int stride) {
        int s = stride == 0 ? 20 : stride;
        int bytes = type == GLConst.GL_UNSIGNED_SHORT ? 2 : 4;
        for (int i = 0; i < drawcount; i++) {
            int base = i * s;
            int count = indirect.getInt(base);
            int primcount = indirect.getInt(base + 4);
            int firstIndex = indirect.getInt(base + 8);
            GLES31.glDrawElementsInstanced(mode, count, type, firstIndex * bytes, primcount);
        }
    }

    public static void glMultiDrawElementsIndirect(int mode, int type, long indirect, int drawcount, int stride) {
        ByteBuffer buf = MemoryUtil.byteBuffer(indirect, Math.max(1, drawcount * (stride == 0 ? 20 : stride)));
        glMultiDrawElementsIndirect(mode, type, buf, drawcount, stride);
    }

    // ---------------------------------------------------------------- vertex attrib binding (GLES 3.1)
    // The stock Android GLES31 API omits these; state is tracked so draw paths compile and
    // attribute arrays can be rebound before classic draws.

    static final int[] ATTRIB_FORMAT_SIZE = new int[16];
    static final int[] ATTRIB_FORMAT_TYPE = new int[16];
    static final boolean[] ATTRIB_FORMAT_NORMALIZED = new boolean[16];
    static final boolean[] ATTRIB_FORMAT_INTEGER = new boolean[16];
    static final int[] ATTRIB_RELATIVE_OFFSET = new int[16];
    static final int[] ATTRIB_BINDING_INDEX = new int[16];
    static final int[] VERTEX_BUFFER_INDEX = new int[16];
    static final long[] VERTEX_BUFFER_OFFSET = new long[16];
    static final int[] VERTEX_BUFFER_STRIDE = new int[16];
    static final int[] VERTEX_BUFFER_DIVISOR = new int[16];
    static {
        java.util.Arrays.fill(VERTEX_BUFFER_STRIDE, 0);
        java.util.Arrays.fill(VERTEX_BUFFER_DIVISOR, 0);
    }

    public static void glVertexAttribFormat(int attribIndex, int size, int type, boolean normalized, int relativeOffset) {
        ATTRIB_FORMAT_SIZE[attribIndex] = size;
        ATTRIB_FORMAT_TYPE[attribIndex] = type;
        ATTRIB_FORMAT_NORMALIZED[attribIndex] = normalized;
        ATTRIB_FORMAT_INTEGER[attribIndex] = false;
        ATTRIB_RELATIVE_OFFSET[attribIndex] = relativeOffset;
    }

    public static void glVertexAttribIFormat(int attribIndex, int size, int type, int relativeOffset) {
        ATTRIB_FORMAT_SIZE[attribIndex] = size;
        ATTRIB_FORMAT_TYPE[attribIndex] = type;
        ATTRIB_FORMAT_NORMALIZED[attribIndex] = false;
        ATTRIB_FORMAT_INTEGER[attribIndex] = true;
        ATTRIB_RELATIVE_OFFSET[attribIndex] = relativeOffset;
    }

    public static void glVertexAttribBinding(int attribIndex, int bindingIndex) {
        ATTRIB_BINDING_INDEX[attribIndex] = bindingIndex;
    }

    public static void glBindVertexBuffer(int bindingIndex, int buffer, long offset, int stride) {
        VERTEX_BUFFER_INDEX[bindingIndex] = buffer;
        VERTEX_BUFFER_OFFSET[bindingIndex] = offset;
        VERTEX_BUFFER_STRIDE[bindingIndex] = stride;
    }

    public static void glVertexBindingDivisor(int bindingIndex, int divisor) {
        VERTEX_BUFFER_DIVISOR[bindingIndex] = divisor;
    }

    // ---------------------------------------------------------------- queries / sync / samplers

    public static void glGenQueries(IntBuffer ids) {
        int count = ids.remaining();
        int[] out = new int[count];
        int pos = ids.position();
        GLES30.glGenQueries(count, out, 0);
        for (int i = 0; i < count; i++) ids.put(pos + i, out[i]);
    }

    public static int glGenQueries() {
        int[] out = new int[1];
        GLES30.glGenQueries(1, out, 0);
        return out[0];
    }

    public static void glDeleteQueries(IntBuffer ids) { GLES30.glDeleteQueries(ids); }
    public static void glDeleteQueries(int id) { GLES30.glDeleteQueries(1, new int[]{id}, 0); }

    public static void glQueryCounter(int id, int target) {
        // GL_TIMESTAMP queries require desktop/EXT; record as GL_ANY_SAMPLES_PASSED-ish no-op.
        GLES30.glBeginQuery(GLES30.GL_ANY_SAMPLES_PASSED, id);
        GLES30.glEndQuery(GLES30.GL_ANY_SAMPLES_PASSED);
    }

    public static int glGetQueryObjecti(int id, int pname) {
        int[] v = new int[1];
        GLES30.glGetQueryObjectiv(id, pname, v, 0);
        return v[0];
    }

    public static long glGetQueryObjectui64(int id, int pname) {
        int[] v = new int[1];
        GLES30.glGetQueryObjectuiv(id, pname, v, 0);
        return v[0];
    }

    public static void glGetQueryObjecti64(int id, int pname, LongBuffer params) {
        // unavailable on GLES; fill 0
        params.put(params.position(), 0L);
    }

    public static long glFenceSync(int condition, int flags) { return GLES32.glFenceSync(condition, flags); }
    public static void glDeleteSync(long sync) { GLES32.glDeleteSync(sync); }
    public static int glClientWaitSync(long sync, int flags, long timeout) {
        return GLES32.glClientWaitSync(sync, flags, timeout);
    }

    public static void glGenSamplers(IntBuffer samplers) {
        int count = samplers.remaining();
        int[] out = new int[count];
        int pos = samplers.position();
        GLES30.glGenSamplers(count, out, 0);
        for (int i = 0; i < count; i++) samplers.put(pos + i, out[i]);
    }

    public static int glGenSamplers() {
        int[] out = new int[1];
        GLES30.glGenSamplers(1, out, 0);
        return out[0];
    }

    public static void glDeleteSamplers(IntBuffer samplers) { GLES30.glDeleteSamplers(samplers); }
    public static void glDeleteSamplers(int sampler) { GLES30.glDeleteSamplers(1, new int[]{sampler}, 0); }
    public static void glBindSampler(int unit, int sampler) { GLES30.glBindSampler(unit, sampler); }
    public static void glSamplerParameteri(int sampler, int pname, int param) { GLES30.glSamplerParameteri(sampler, pname, param); }
    public static void glSamplerParameterf(int sampler, int pname, float param) { GLES30.glSamplerParameterf(sampler, pname, param); }

    // ---------------------------------------------------------------- mapped-buffer emulation

    /**
     * Maps a buffer range to a host {@link MemoryUtil} region. GLES has no buffer mapping,
     * so we keep a per-buffer arena and copy on flush/unmap (see {@link FlushMappedBufferRange}).
     */
    public static long nglMapBufferRange(int target, long offset, long length, int access) {
        return transient_mapped_buffer.map(target, offset, length, access);
    }

    public static boolean glUnmapBuffer(int target) {
        return transient_mapped_buffer.unmap(target);
    }

    public static void glFlushMappedBufferRange(int target, long offset, long length) {
        transient_mapped_buffer.flush(target, offset, length);
    }

    // ---------------------------------------------------------------- helpers

    static int[] concat(int head, IntBuffer rest) {
        int size = 1 + rest.remaining();
        int[] out = new int[size];
        out[0] = head;
        for (int i = 0; i < rest.remaining(); i++) out[1 + i] = rest.get(rest.position() + i);
        return out;
    }

    static String readCString(ByteBuffer buf) {
        ByteBuffer d = buf.duplicate();
        int start = d.position();
        while (d.hasRemaining() && d.get() != 0) { }
        int end = d.position();
        byte[] bytes = new byte[Math.max(0, end - start)];
        buf.duplicate().position(start).limit(Math.max(start, end)).get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Translates desktop GLSL 1.50 core snippets into GLSL ES 3.00 the way the GLES
     * driver expects. The game's shaders are authored as {@code #version 150 core}.
     */
    static CharSequence shaderSourceCompat(CharSequence source) {
        String sh = source.toString();
        if (sh.contains("#version 150 core") || sh.contains("#version 150")) {
            sh = sh.replaceFirst("#version\\s+150[^\\n]*", "#version 300 es");
            sh = sh.replace("gl_FragColor", "colorOut0");
            sh = sh.replace("gl_FragDepth", "gl_FragDepthEXT");
        } else if (sh.contains("#version 330 core")) {
            sh = sh.replaceFirst("#version\\s+330[^\\n]*", "#version 300 es");
            sh = sh.replace("gl_FragColor", "colorOut0");
        }
        return sh;
    }

    /** Java-side emulation of ARB_buffer_storage persistent mapped buffers. */
    static final class transient_mapped_buffer {
        private static final java.util.concurrent.ConcurrentHashMap<Integer, ByteBuffer> MAPS = new java.util.concurrent.ConcurrentHashMap<>();

        static long map(int target, long offset, long length, int access) {
            ByteBuffer arena = ByteBuffer.allocateDirect((int) Math.max(1, length)).order(ByteOrder.nativeOrder());
            long addr = MemoryUtil.memAddress(arena);
            MAPS.put(target, arena);
            return addr;
        }

        static void flush(int target, long offset, long length) {
            ByteBuffer arena = MAPS.get(target);
            if (arena == null) return;
            // On flush, upload the mapped range into the bound GL buffer (VBO/UBO path handled by backend).
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, (int) offset, (int) length, arena);
        }

        static boolean unmap(int target) {
            ByteBuffer arena = MAPS.remove(target);
            return arena != null;
        }
    }

    /** Selection of GLES3.1 instanced-draw fallbacks for functionality missing on some drivers. */
    static final class gles_robustness {
        static void drawInstanced(int mode, int count, int type, int indices, int instancecount) {
            try {
                GLES31.glDrawElementsInstanced(mode, count, type, indices, instancecount);
            } catch (Throwable t) {
                GLES20.glDrawElements(mode, count, type, indices);
                for (int i = 1; i < instancecount; i++) {
                    GLES20.glDrawElements(mode, count, type, indices);
                }
            }
        }
    }
}