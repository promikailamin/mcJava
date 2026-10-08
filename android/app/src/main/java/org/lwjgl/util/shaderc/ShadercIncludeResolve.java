package org.lwjgl.util.shaderc;

/**
 * Include resolution callback fired by the (shaderc) GLSL preprocessor. In this port the
 * callback results come from the game's own {@link ShaderSource} cache, so the interface
 * mirrors LWJGL's three-arg callback contract with a pointer-buffer output slot.
 */
public interface ShadercIncludeResolve {

    long invoke(long userdata, long requested_source, int max_include_depth, long include_callback_userdata,
                org.lwjgl.PointerBuffer output);

    static ShadercIncludeResolve create(ShadercIncludeResolve callback) {
        return callback;
    }
}