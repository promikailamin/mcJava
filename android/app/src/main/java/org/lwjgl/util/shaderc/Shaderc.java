package org.lwjgl.util.shaderc;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

/**
 * {@code Shaderc} (glslang) native surface used by {@code GlslCompiler}.
 *
 * <p>The GL backend cross-compiles GLSL → SPIR-V through this native in upstream Minecraft.
 * Building shaderc for Android is out of scope for this first port (see README), so every
 * compilation entry point throws; the class still loads (non-null library handle) so
 * {@code NativeLibrariesBootstrap} passes.</p>
 */
public final class Shaderc {

    public static final int shaderc_compilation_status_success = 0;
    public static final int shaderc_source_language_hlsl = 1;
    public static final int shaderc_shader_stage_vertex = 0;
    public static final int shaderc_shader_stage_fragment = 4;
    public static final int shaderc_optimization_level_performance = 1;

    private static final Object LIBRARY = new Object();

    private Shaderc() {
    }

    public static Object getLibrary() {
        return LIBRARY;
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(
                "Shaderc (glslang) native not available on Android yet — see README. Shader compilation requires a native build of shaderc/api/shaderc.");
    }

    public static long shaderc_compiler_initialize() {
        throw unsupported();
    }

    public static void shaderc_compiler_release(long compiler) {
        throw unsupported();
    }

    public static long shaderc_compile_options_initialize() {
        throw unsupported();
    }

    public static void shaderc_compile_options_release(long options) {
        throw unsupported();
    }

    public static void shaderc_compile_options_set_target_env(long options, int target_env, int version) {
        throw unsupported();
    }

    public static void shaderc_compile_options_set_auto_bind_uniforms(long options, boolean auto) {
        throw unsupported();
    }

    public static void shaderc_compile_options_set_preserve_bindings(long options, boolean preserve) {
        throw unsupported();
    }

    public static void shaderc_compile_options_set_generate_debug_info(long options) {
        throw unsupported();
    }

    public static void shaderc_compile_options_set_optimization_level(long options, int level) {
        throw unsupported();
    }

    public static void shaderc_compile_options_add_macro_definition(long options, CharSequence name, CharSequence value) {
        throw unsupported();
    }

    public static void shaderc_compile_options_set_include_callbacks(long options, ShadercIncludeResolve resolve,
                                                                     ShadercIncludeResultRelease release, long userData) {
        throw unsupported();
    }

    public static long shaderc_compile_into_spv(long compiler, long source, int shaderKind, long fileName,
                                                long entryPointName, long options) {
        throw unsupported();
    }

    public static int shaderc_result_get_compilation_status(long result) {
        throw unsupported();
    }

    public static ByteBuffer shaderc_result_get_bytes(long result) {
        throw unsupported();
    }

    public static long shaderc_result_get_error_message(long result) {
        throw unsupported();
    }

    public static int shaderc_result_get_num_errors(long result) {
        throw unsupported();
    }

    public static int shaderc_result_get_num_warnings(long result) {
        throw unsupported();
    }

    public static void shaderc_result_release(long result) {
        throw unsupported();
    }
}