package org.lwjgl.sdl;

import android.util.Log;

@FunctionalInterface
public interface SDL_LogOutputFunction {

    /** Mirrors {@code SDL_LogOutputFunction}: user data, category, priority, message address. */
    void invoke(long userData, int category, int priority, long message);

    static SDL_LogOutputFunction create(SDL_LogOutputFunction callback) {
        return callback;
    }

    default void close() {
    }
}