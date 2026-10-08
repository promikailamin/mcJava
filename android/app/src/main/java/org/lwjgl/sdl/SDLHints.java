package org.lwjgl.sdl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SDLHints {

    private static final Map<String, String> HINTS = new ConcurrentHashMap<>();

    private SDLHints() {
    }

    public static boolean SDL_SetHint(String name, String value) {
        HINTS.put(name, value);
        return true;
    }

    public static String SDL_GetHint(String name) {
        return HINTS.get(name);
    }
}