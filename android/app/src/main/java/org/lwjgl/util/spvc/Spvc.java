package org.lwjgl.util.spvc;

import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;

/**
 * {@code SPIRV-Cross} surface used by {@code GlPipelineRecompiler} / {@code SPIRVModule}.
 *
 * <p>Compiling SPIRV-Cross for Android is NDK work tracked in the README; until then the
 * shim succeeds with empty resource lists so shader reflection degrades gracefully. Shader
 * compilation cannot run without the real library.</p>
 */
public final class Spvc {

    public static final int SPVC_BASETYPE_UNKNOWN = 0;
    public static final int SPVC_BASETYPE_UINT = 1;
    public static final int SPVC_BASETYPE_INT = 2;
    public static final int SPVC_BASETYPE_FLOAT = 3;
    public static final int SPVC_BASETYPE_VOID = 4;
    public static final int SPVC_BASETYPE_STRUCT = 5;
    public static final int SPVC_BASETYPE_BOOL = 6;
    public static final int SPVC_DIM_1D = 0;
    public static final int SPVC_DIM_2D = 1;
    public static final int SPVC_RESOURCE_TYPE_UNIFORM_BUFFER = 0;
    public static final int SPVC_RESOURCE_TYPE_STORAGE_BUFFER = 1;
    public static final int SPVC_RESOURCE_TYPE_SAMPLER = 2;
    public static final int SPVC_RESOURCE_TYPE_TEXTURE = 3;

    private Spvc() {
    }

    public static int spvc_context_create(PointerBuffer out) {
        if (out.capacity() > 0) out.put(0, 0L);
        return 0;
    }

    public static int spvc_context_parse_spirv(long context, IntBuffer spirv, int wordCount, PointerBuffer out) {
        if (out.capacity() > 0) out.put(0, 0L);
        return 0;
    }

    public static int spvc_context_create_compiler(long context, int sourceLanguage, long ir, int flags, PointerBuffer out) {
        if (out.capacity() > 0) out.put(0, 0L);
        return 0;
    }

    public static void spvc_context_destroy(long context) {
    }

    public static int spvc_compiler_create_shader_resources(long compiler, PointerBuffer out) {
        if (out.capacity() > 0) out.put(0, 0L);
        return 0;
    }

    public static int spvc_resources_get_resource_list_for_type(long resources, int type, PointerBuffer list, PointerBuffer count) {
        if (list.capacity() > 0) list.put(0, 0L);
        if (count.capacity() > 0) count.put(0, 0L);
        return 0;
    }

    public static int spvc_compiler_get_decoration(long compiler, long id, int decoration) {
        return 0;
    }

    public static String spvc_compiler_get_name(long compiler, long id) {
        return null;
    }

    public static int spvc_compiler_set_name(long compiler, long id, CharSequence name) {
        return 0;
    }

    public static int spvc_compiler_set_entry_point(long compiler, CharSequence name, int executionModel) {
        return 0;
    }

    public static int spvc_compiler_create_compiler_options(long compiler, PointerBuffer out) {
        if (out.capacity() > 0) out.put(0, 0L);
        return 0;
    }

    public static int spvc_compiler_install_compiler_options(long compiler, long options) {
        return 0;
    }

    public static int spvc_compiler_options_set_bool(long options, int option, boolean value) {
        return 0;
    }

    public static int spvc_compiler_options_set_uint(long options, int option, int value) {
        return 0;
    }

    public static int spvc_compiler_compile(long compiler, PointerBuffer out) {
        if (out.capacity() > 0) out.put(0, 0L);
        return 0;
    }

    public static boolean spvc_compiler_get_binary_offset_for_decoration(long compiler, long id, int decoration, IntBuffer offset) {
        return false;
    }

    public static long spvc_compiler_get_type_handle(long compiler, long id) {
        return 0L;
    }

    public static long spvc_compiler_get_declared_struct_size(long compiler, long the_type) {
        return 0L;
    }

    public static int spvc_type_get_basetype(long type) {
        return SPVC_BASETYPE_UNKNOWN;
    }

    public static int spvc_type_get_vector_size(long type) {
        return 0;
    }

    public static int spvc_type_get_num_array_dimensions(long type) {
        return 0;
    }

    public static int spvc_type_get_array_dimension(long type, int index) {
        return 0;
    }

    public static int spvc_type_get_image_dimension(long type) {
        return SPVC_DIM_2D;
    }

    public static int spvc_type_get_image_sampled_type(long type) {
        return SPVC_BASETYPE_FLOAT;
    }
}