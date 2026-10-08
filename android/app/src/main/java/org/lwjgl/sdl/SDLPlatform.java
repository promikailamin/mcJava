package org.lwjgl.sdl;

public final class SDLPlatform {

    private SDLPlatform() {
    }

    public static String SDL_GetPlatform() {
        return "Android";
    }
}