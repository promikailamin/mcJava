package org.lwjgl.sdl;

import org.lwjgl.system.MemoryStack;

public final class SDLPixels {

    private SDLPixels() {
    }

    public static SDL_PixelFormatDetails SDL_GetPixelFormatDetails(int format) {
        SDL_PixelFormatDetails d = SDL_PixelFormatDetails.calloc();
        d.format(format).bitsPerPixel(32).Rbits(8).Gbits(8).Bbits(8).Abits(8);
        return d;
    }

    public static int SDL_GetPixelFormatFormat(int format) {
        return format;
    }
}