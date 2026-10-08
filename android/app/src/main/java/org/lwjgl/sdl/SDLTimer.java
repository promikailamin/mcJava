package org.lwjgl.sdl;

import android.os.SystemClock;

public final class SDLTimer {

    private SDLTimer() {
    }

    public static long SDL_GetTicks() {
        return SystemClock.uptimeMillis();
    }

    public static long SDL_GetTicksNS() {
        return SystemClock.elapsedRealtimeNanos();
    }

    public static boolean SDL_Delay(int ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}