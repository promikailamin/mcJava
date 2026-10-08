package pro.minecraft.host;

import java.util.concurrent.atomic.AtomicBoolean;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES32;
import android.util.Log;
import android.view.Surface;

/**
 * OpenGL ES 3.2 context owner for the engine thread.
 *
 * Emulates LWJGL's "window + GL context" pair: the context is created against the
 * {@link game_view} surface and made current on the game thread, so the game's GL calls
 * (bridged through {@code org.lwjgl.opengl.GL33C}) run on the same thread that presents.
 */
public final class game_renderer {

    private static final String TAG = "game_renderer";

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int EGL_OPENGL_ES3_BIT_KHR = 0x0040;
    private static final int EGL_RENDERABLE_TYPE = 0x3040;
    private static final int EGL_DEPTH_SIZE = 0x3025;
    private static final int EGL_STENCIL_SIZE = 0x3026;
    private static final int EGL_SURFACE_TYPE = 0x3033;
    private static final int EGL_WINDOW_BIT = 0x0004;

    private final game_view view;

    private EGLDisplay display;
    private EGLContext context;
    private EGLSurface eglSurface;
    private volatile Surface surface;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private int width;
    private int height;

    public game_renderer(game_view view) {
        this.view = view;
    }

    /** Stores the Android surface handed to the engine thread. */
    public void attach(Surface surface) {
        this.surface = surface;
    }

    public Surface surface() {
        return surface;
    }

    /** Waits until the view has an Android surface. */
    public Surface await_surface() {
        while (surface == null && !destroyed.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            surface = view.getHolder().getSurface();
        }
        return surface;
    }

    /** Must run on the engine thread. Returns false if GLES3.2 was unavailable. */
    public boolean create_context() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (display == EGL14.EGL_NO_DISPLAY) {
            return false;
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            Log.e(TAG, "eglInitialize failed");
            return false;
        }

        int[] attribs = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL_DEPTH_SIZE, 24,
                EGL_STENCIL_SIZE, 8,
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfig, 0) || numConfig[0] == 0) {
            Log.e(TAG, "eglChooseConfig failed");
            return false;
        }
        EGLConfig config = configs[0];

        int[] ctxAttribs = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE };
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0);
        if (context == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "eglCreateContext failed");
            return false;
        }

        Surface target = await_surface();
        if (target == null) {
            return false;
        }
        eglSurface = EGL14.eglCreateWindowSurface(display, config, target, new int[]{EGL14.EGL_NONE}, 0);
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "eglCreateWindowSurface failed");
            return false;
        }

        if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
            Log.e(TAG, "eglMakeCurrent failed: " + EGL14.eglGetError());
            return false;
        }
        EGL14.eglQuerySurface(display, eglSurface, EGL14.EGL_WIDTH, new int[]{0}, 0);
        return true;
    }

    public int swap() {
        if (eglSurface == null) {
            return 0;
        }
        EGL14.eglSwapBuffers(display, eglSurface);
        return EGL14.eglGetError();
    }

    public void destroy() {
        destroyed.set(true);
        if (display != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(display, eglSurface);
            eglSurface = EGL14.EGL_NO_SURFACE;
        }
        if (display != EGL14.EGL_NO_DISPLAY && context != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context);
            context = EGL14.EGL_NO_CONTEXT;
        }
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(display);
            display = EGL14.EGL_NO_DISPLAY;
        }
    }
}