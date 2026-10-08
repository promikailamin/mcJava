package org.lwjgl.sdl;

/** Simple error surface ({@code SDLError} bindings). */
public final class SDLError {

    private static final ThreadLocal<String> ERROR = ThreadLocal.withInitial(() -> "No error");

    private SDLError() {
    }

    public static void SDL_ClearError() {
        ERROR.set("No error");
    }

    public static String SDL_GetError() {
        return ERROR.get();
    }

    public static void setError(String message) {
        ERROR.set(message);
    }
}