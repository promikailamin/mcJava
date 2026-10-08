package org.lwjgl.sdl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Mouse surface backed by touch deltas from {@code game_input}. */
public final class SDLMouse {

    private static volatile double x;
    private static volatile double y;

    private static final Map<String, Long> CURSORS = new ConcurrentHashMap<>();
    private static long defaultCursor;

    public static final int SDL_SYSTEM_CURSOR_ARROW = 0;
    public static final int SDL_SYSTEM_CURSOR_IBEAM = 1;
    public static final int SDL_SYSTEM_CURSOR_HAND = 3;
    public static final int SDL_SYSTEM_CURSOR_CROSSHAIR = 2;
    public static final int SDL_SYSTEM_CURSOR_SIZEALL = 4;
    public static final int SDL_SYSTEM_CURSOR_SIZENS = 8;
    public static final int SDL_SYSTEM_CURSOR_SIZEWE = 9;
    public static final int SDL_SYSTEM_CURSOR_NOT_ALLOWED = 10;

    private SDLMouse() {
    }

    public static void setPosition(double nx, double ny) {
        x = nx;
        y = ny;
    }

    public static int SDL_GetMouseState(float[] xOut, float[] yOut) {
        if (xOut != null) xOut[0] = (float) x;
        if (yOut != null) yOut[0] = (float) y;
        return 0;
    }

    public static int SDL_GetMouseState(java.nio.FloatBuffer xOut, java.nio.FloatBuffer yOut) {
        if (xOut != null) xOut.put(0, (float) x);
        if (yOut != null) yOut.put(0, (float) y);
        return 0;
    }

    public static int SDL_GetGlobalMouseState(float[] xOut, float[] yOut) {
        return SDL_GetMouseState(xOut, yOut);
    }

    public static int SDL_GetGlobalMouseState(java.nio.FloatBuffer xOut, java.nio.FloatBuffer yOut) {
        return SDL_GetMouseState(xOut, yOut);
    }

    public static boolean SDL_WarpMouseInWindow(long window, double nx, double ny) {
        x = nx;
        y = ny;
        return true;
    }

    public static boolean SDL_WarpMouseInWindow(long window, float nx, float ny) {
        x = nx;
        y = ny;
        return true;
    }

    public static boolean SDL_SetWindowRelativeMouseMode(long window, boolean enabled) {
        return true;
    }

    public static boolean SDL_SetCursor(long cursor) {
        return true;
    }

    public static long SDL_CreateSystemCursor(int id) {
        return CURSORS.computeIfAbsent("sys:" + id, k -> ++SDLMouseCursorHandle);
    }

    public static long SDL_GetDefaultCursor() {
        if (defaultCursor == 0L) {
            defaultCursor = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_ARROW);
        }
        return defaultCursor;
    }

    public static long SDL_GetCursor() {
        return SDL_GetDefaultCursor();
    }

    private static long SDLMouseCursorHandle = 0x400000L;
}