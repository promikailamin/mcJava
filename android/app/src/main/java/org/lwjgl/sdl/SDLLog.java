package org.lwjgl.sdl;

import android.util.Log;

public final class SDLLog {

    private static volatile SDL_LogOutputFunction hook;
    private static volatile int priority = 4;

    private SDLLog() {
    }

    public static boolean SDL_SetLogOutputFunction(SDL_LogOutputFunction callback, long userdata) {
        hook = callback;
        return true;
    }

    public static void SDL_SetLogPriorities(int newPriority) {
        priority = newPriority;
    }

    public static void SDL_Log(String message) {
        sink(1, 3, message);
    }

    public static void SDL_LogError(int category, String message) {
        sink(category, 2, message);
    }

    public static void SDL_LogWarn(int category, String message) {
        sink(category, 5, message);
    }

    private static void sink(int category, int prio, String message) {
        Log.d("lwjgl.sdl", "[" + category + "/" + prio + "] " + message);
        if (hook != null) {
            hook.invoke(0L, category, prio, addressOf(message));
        }
    }

    private static long addressOf(String message) {
        return System.identityHashCode(message) + 1L;
    }
}