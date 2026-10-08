package org.lwjgl.opengl;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Simulated GL capabilities. All flags the game queries default to {@code true}
 * because the GLES 3.2 bridge implements (or emulates) every capability.
 */
public class GLCapabilities {

    public final java.util.concurrent.locks.Lock lock = new ReentrantLock();

    // Basic GL version gates
    public boolean OpenGL11 = true, OpenGL12 = true, OpenGL13 = true, OpenGL14 = true,
            OpenGL15 = true, OpenGL20 = true, OpenGL21 = true, OpenGL30 = true,
            OpenGL31 = true, OpenGL32 = true, OpenGL33 = true, OpenGL40 = true,
            OpenGL41 = false, OpenGL42 = false, OpenGL43 = false, OpenGL44 = false, OpenGL45 = false;

    // Memory mapping via ARB_buffer_storage-style fallback
    public boolean GL_ARB_buffer_storage = true;
    public boolean GL_ARB_direct_state_access = true;
    public boolean GL_ARB_clip_control = true;
    public boolean GL_ARB_shader_draw_parameters = true;
    public boolean GL_ARB_draw_indirect = true;
    public boolean GL_ARB_multi_draw_indirect = true;
    public boolean GL_ARB_base_instance = true;
    public boolean GL_ARB_vertex_attrib_binding = true;
    public boolean GL_EXT_texture_filter_anisotropic = true;
    public boolean GL_ARB_debug_output = true;
    public boolean GL_EXT_debug_label = true;
    public boolean GL_KHR_debug = true;
    public boolean GL_ARB_multisample = true;
    public boolean GL_ARB_shader_objects = true;
    public boolean GL_ARB_framebuffer_object = true;
    public boolean GL_ARB_ES2_compatibility = true;
    public boolean GL_ARB_vertex_array_object = true;
    public boolean GL_ARB_sync = true;
    public boolean GL_ARB_timer_query = true;
    public boolean GL_ARB_sampler_objects = true;
    public boolean GL_ARB_seamless_cube_map = true;
    public boolean GL_ARB_texture_float = true;
    public boolean GL_ARB_texture_rg = true;
    public boolean GL_ARB_texture_rgb10_a2ui = true;
    public boolean GL_ARB_texture_non_power_of_two = true;
    public boolean GL_EXT_framebuffer_object = true;
    public boolean GL_EXT_framebuffer_blit = true;
    public boolean GL_EXT_debug_marker = true;

    public int majorVersion = 3;
    public int minorVersion = 2;

    public GLCapabilities() {
    }

    public boolean supportsGL(int major, int minor) {
        return majorVersion > major
                || (majorVersion == major && minorVersion >= minor);
    }
}