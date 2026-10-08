package org.lwjgl.sdl;

public final class SDLInit {

    public static final int SDL_INIT_TIMER = 0x00000001;
    public static final int SDL_INIT_AUDIO = 0x00000010;
    public static final int SDL_INIT_VIDEO = 0x00000020;
    public static final int SDL_INIT_EVENTS = 0x00004000;

    static long nextWindow = 0x1000L;

    private SDLInit() {
    }

    public static boolean SDL_Init(int flags) {
        return true;
    }

    public static boolean SDL_SetAppMetadata(String appName, String appVersion, String appIdentifier) {
        return true;
    }

    public static boolean SDL_SetAppMetadataProperty(String name, String value) {
        return true;
    }

    public static void SDL_Quit() {
    }
}