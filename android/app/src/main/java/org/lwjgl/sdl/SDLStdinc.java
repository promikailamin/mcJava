package org.lwjgl.sdl;

import org.lwjgl.PointerBuffer;
import java.nio.Buffer;

public final class SDLStdinc {

    private SDLStdinc() {
    }

    public static void SDL_free(long address) {
    }

    public static void SDL_free(PointerBuffer pointer) {
    }

    public static void SDL_free(Buffer buffer) {
    }

    public static long SDL_malloc(long size) {
        return org.lwjgl.system.MemoryUtil.nalloc(size);
    }
}