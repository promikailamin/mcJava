package org.lwjgl.sdl;

/** Pixel-conversion surface ({@code SDLSurface} bindings). Only icons use it. */
public final class SDLSurface {

    public static final int SDL_PIXELFORMAT_UNKNOWN = 0;
    public static final int SDL_PIXELFORMAT_RGBA32 = 376840196;

    private static long nextHandle = 0x200000L;

    static long nextSurface() {
        return ++nextHandle;
    }

    private SDLSurface() {
    }

    public static SDL_Surface SDL_CreateSurfaceFrom(int width, int height, int format, java.nio.ByteBuffer pixels, int pitch) {
        SDL_Surface surface = SDL_Surface.create();
        surface.setSize(width, height);
        return surface;
    }

    public static boolean SDL_AddSurfaceAlternateImage(SDL_Surface surface, SDL_Surface other) {
        return true;
    }

    public static void SDL_DestroySurface(SDL_Surface surface) {
    }
}