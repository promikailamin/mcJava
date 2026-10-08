package org.lwjgl.sdl;

import org.lwjgl.system.MemoryStack;

public final class SDLPixels {

    private SDLPixels() {
    }

    public static SDL_PixelFormatDetails SDL_GetPixelFormatDetails(int format) {
        SDL_PixelFormatDetails d = SDL_PixelFormatDetails.calloc();
        d.setFormat(format).setBitsPerPixel(32).setRbits(8).setGbits(8).setBbits(8).setAbits(8);
        return d;
    }

    public static int SDL_GetPixelFormatFormat(int format) {
        return format;
    }
}