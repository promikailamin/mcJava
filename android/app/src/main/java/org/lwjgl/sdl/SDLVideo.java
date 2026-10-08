package org.lwjgl.sdl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.Library;

/**
 * SDL video/window driver backed by the Android activity surface. The GL context is the
 * activity's GLSurfaceView context; SDL GL calls delegate to it or are accepted as no-ops
 * so the game's window bootstrap succeeds. No native SDL is involved.
 */
public final class SDLVideo {

    private static final Map<Long, sdl_window> WINDOWS = new ConcurrentHashMap<>();
    private static final FunctionProvider GL_PROVIDER = name -> Library.functionHandle(name);

    public static final long SDL_WINDOW_FULLSCREEN = 0x00000001L;
    public static final long SDL_WINDOW_OPENGL = 0x00000002L;
    public static final long SDL_WINDOW_HIDDEN = 0x00000008L;
    public static final long SDL_WINDOW_BORDERLESS = 0x00000010L;
    public static final long SDL_WINDOW_RESIZABLE = 0x00000020L;
    public static final long SDL_WINDOW_MOUSE_GRABBED = 0x00000400L;
    public static final long SDL_WINDOW_HIGH_PIXEL_DENSITY = 0x00002000L;

    // GL attributes mirrored by the surface config
    public static final int SDL_GL_CONTEXT_MAJOR_VERSION = 0;
    public static final int SDL_GL_CONTEXT_MINOR_VERSION = 1;
    public static final int SDL_GL_CONTEXT_PROFILE_MASK = 2;
    public static final int SDL_GL_CONTEXT_FLAGS = 6;
    public static final int SDL_GL_CONTEXT_DEBUG_FLAG = 0x0002;
    public static final int SDL_GL_SHARE_WITH_CURRENT_CONTEXT = 7;
    public static final int SDL_GL_FRAMEBUFFER_SRGB_CAPABLE = 10;

    private static int surfaceWidth = 960;
    private static int surfaceHeight = 540;
    private static float pixelDensity = 1.0f;
    private static volatile boolean framePresented;
    private static final Object FRAME_LOCK = new Object();
    private static volatile android.view.Surface androidSurface;
    private static volatile java.util.function.Supplier<Integer> swapFunction = () -> 0;

    private SDLVideo() {
    }

    static volatile long CURRENT_WINDOW;

    /** Display surface mirror updated by the host activity. */
    public static void updateDisplay(int width, int height, float density) {
        surfaceWidth = Math.max(1, width);
        surfaceHeight = Math.max(1, height);
        pixelDensity = density;
        WINDOWS.values().forEach(w -> w.updateSize(surfaceWidth, surfaceHeight));
    }

    /** Block until the host signals the current frame was presented. */
    public static void awaitFrame() {
        synchronized (FRAME_LOCK) {
            while (!framePresented) {
                try {
                    FRAME_LOCK.wait(250L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            framePresented = false;
        }
    }

    public static void onFramePresented() {
        synchronized (FRAME_LOCK) {
            framePresented = true;
            FRAME_LOCK.notifyAll();
        }
    }

    public static int getSurfaceWidth() { return surfaceWidth; }
    public static int getSurfaceHeight() { return surfaceHeight; }

    /** Android surface the engine thread renders to. */
    public static void set_surface(android.view.Surface surface) {
        androidSurface = surface;
    }

    public static android.view.Surface get_surface() {
        return androidSurface;
    }

    /** Engine presents via {@code game_renderer.swap()} — the {@code eglSwapBuffers} call. */
    public static void set_swap_function(java.util.function.Supplier<Integer> swap) {
        swapFunction = swap;
    }

    public static long SDL_CreateWindow(CharSequence title, int x, int y, int w, int h, long flags) {
        sdl_window win = new sdl_window(title.toString(), surfaceWidth, surfaceHeight);
        WINDOWS.put(win.handle, win);
        CURRENT_WINDOW = win.handle;
        return win.handle;
    }

    public static void SDL_DestroyWindow(long window) {
        WINDOWS.remove(window);
        if (CURRENT_WINDOW == window) {
            CURRENT_WINDOW = 0L;
        }
    }

    public static long SDL_GL_LoadLibrary(CharSequence path) {
        return 0L;
    }

    public static long SDL_GL_GetProcAddress(CharSequence name) {
        return GL_PROVIDER.getFunctionAddress(name);
    }

    public static boolean SDL_GL_SetAttribute(int attr, int value) { return true; }

    public static int SDL_GL_GetAttribute(int attr, int[] value) {
        switch (attr) {
            case SDL_GL_CONTEXT_MAJOR_VERSION -> value[0] = 3;
            case SDL_GL_CONTEXT_MINOR_VERSION -> value[0] = 2;
            case SDL_GL_CONTEXT_PROFILE_MASK -> value[0] = 1;
            default -> value[0] = 0;
        }
        return 0;
    }

    public static int SDL_GL_GetAttribute(int attr, java.nio.IntBuffer value) {
        int[] tmp = new int[1];
        int res = SDL_GL_GetAttribute(attr, tmp);
        if (value.hasRemaining()) {
            value.put(0, tmp[0]);
        }
        return res;
    }

    public static long SDL_GL_CreateContext(long window) {
        return WINDOWS.containsKey(window) ? window : 0L;
    }

    public static boolean SDL_GL_MakeCurrent(long window, long context) { return true; }

    public static boolean SDL_GL_SetSwapInterval(int interval) { return true; }

    public static void SDL_GL_SwapWindow(long window) {
        swapFunction.get();
        onFramePresented();
    }

    public static void SDL_GL_DestroyContext(long context) {
    }

    public static void SDL_GL_UnloadLibrary() {
    }

    public static int SDL_GetPrimaryDisplay() { return 1; }

    public static int SDL_GetDisplayForWindow(long window) { return 1; }

    public static String SDL_GetCurrentVideoDriver() { return "opencode-es"; }

    public static boolean SDL_GetWindowPosition(long window, int[] x, int[] y) {
        sdl_window w = WINDOWS.get(window);
        if (w == null) return false;
        x[0] = 0; y[0] = 0;
        return true;
    }

    public static boolean SDL_GetWindowPosition(long window, java.nio.IntBuffer x, java.nio.IntBuffer y) {
        sdl_window w = WINDOWS.get(window);
        if (w == null) return false;
        x.put(0, 0);
        y.put(0, 0);
        return true;
    }

    public static boolean SDL_GetWindowSizeInPixels(long window, int[] w, int[] h) {
        sdl_window win = WINDOWS.get(window);
        if (win == null) return false;
        w[0] = win.width; h[0] = win.height;
        return true;
    }

    public static boolean SDL_GetWindowSizeInPixels(long window, java.nio.IntBuffer w, java.nio.IntBuffer h) {
        sdl_window win = WINDOWS.get(window);
        if (win == null) return false;
        w.put(0, win.width);
        h.put(0, win.height);
        return true;
    }

    public static boolean SDL_SetWindowSize(long window, int w, int h) {
        sdl_window win = WINDOWS.get(window);
        if (win != null) win.resize(w, h);
        return true;
    }

    public static boolean SDL_SetWindowPosition(long window, int x, int y) { return true; }

    public static boolean SDL_SetWindowMinimumSize(long window, int minW, int minH) { return true; }

    public static boolean SDL_SetWindowMaximumSize(long window, int maxW, int maxH) { return true; }

    public static boolean SDL_SetWindowBordered(long window, boolean bordered) { return true; }

    public static boolean SDL_SetWindowFullscreen(long window, boolean fullscreen) { return true; }

    public static boolean SDL_SetWindowFullscreenMode(long window, SDL_DisplayMode mode) { return true; }

    public static SDL_DisplayMode SDL_GetWindowFullscreenMode(long window) { return null; }

    public static boolean SDL_SetWindowMouseGrab(long window, boolean grabbed) { return true; }

    public static boolean SDL_SetWindowTitle(long window, CharSequence title) {
        sdl_window w = WINDOWS.get(window);
        if (w != null) w.title = title.toString();
        return true;
    }

    public static long SDL_GetWindowFlags(long window) {
        return SDL_WINDOW_OPENGL | SDL_WINDOW_HIGH_PIXEL_DENSITY | SDL_WINDOW_RESIZABLE;
    }

    public static float SDL_GetWindowPixelDensity(long window) { return pixelDensity; }

    public static boolean SDL_RestoreWindow(long window) { return true; }

    public static boolean SDL_SyncWindow(long window) { return true; }

    public static boolean SDL_SetWindowIcon(long window, SDL_Surface icon) { return true; }

    public static boolean SDL_GetDisplayBounds(int display, SDL_Rect rect) {
        rect.x(0).y(0).w(surfaceWidth).h(surfaceHeight);
        return true;
    }

    public static int SDL_GetDisplays(int[] count) {
        if (count != null) count[0] = 1;
        return 1;
    }

    public static java.nio.IntBuffer SDL_GetDisplays() {
        java.nio.IntBuffer displays = java.nio.ByteBuffer.allocateDirect(4).order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();
        displays.put(0, 1);
        return displays;
    }

    public static String SDL_GetDisplayName(int displayId) { return "Android display"; }

    public static SDL_DisplayMode SDL_GetCurrentDisplayMode(int displayId) {
        SDL_DisplayMode m = SDL_DisplayMode.calloc();
        m.w(surfaceWidth).h(surfaceHeight).refreshRate(60).format(SDLSurface.SDL_PIXELFORMAT_RGBA32);
        return m;
    }

    public static SDL_DisplayMode SDL_GetDesktopDisplayMode(int displayId) {
        return SDL_GetCurrentDisplayMode(displayId);
    }

    public static boolean SDL_GetClosestFullscreenDisplayMode(int displayId, int w, int h,
                                                              int refreshRate, boolean includeHighDensity,
                                                              SDL_DisplayMode mode) {
        mode.w(surfaceWidth).h(surfaceHeight).refreshRate(60).format(SDLSurface.SDL_PIXELFORMAT_RGBA32);
        return true;
    }

    public static int SDL_GetFullscreenDisplayModes(int displayId, int[] count) {
        if (count != null) count[0] = 1;
        return 1;
    }

    /** Returns a pointer array of one fake {@code SDL_DisplayMode} usable with {@code SDL_DisplayMode.create}. */
    public static org.lwjgl.PointerBuffer SDL_GetFullscreenDisplayModes(int displayId) {
        org.lwjgl.PointerBuffer modes = org.lwjgl.PointerBuffer.allocateDirect(1);
        modes.put(0, 0x1F000000001L + displayId);
        return modes;
    }

    private static final class sdl_window {
        final long handle;
        volatile String title;
        volatile int width;
        volatile int height;

        sdl_window(String title, int width, int height) {
            this.handle = ++SDLInit.nextWindow;
            this.title = title;
            this.width = width;
            this.height = height;
        }

        void updateSize(int w, int h) {
            this.width = w;
            this.height = h;
        }

        void resize(int w, int h) {
            updateSize(w, h);
        }
    }
}