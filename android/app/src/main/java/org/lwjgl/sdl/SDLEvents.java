package org.lwjgl.sdl;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Event queue fed from Android input by {@code pro.minecraft.host.sdl_input_bridge}
 * and drained on the game thread via {@code SDL_PollEvent}.
 */
public final class SDLEvents {

    private static final ConcurrentLinkedQueue<SDL_Event> QUEUE = new ConcurrentLinkedQueue<>();

    private SDLEvents() {
    }

    public static void push(SDL_Event event) {
        QUEUE.add(event);
    }

    public static boolean SDL_PumpEvents() {
        return true;
    }

    public static boolean SDL_PollEvent(SDL_Event event) {
        SDL_Event next = QUEUE.poll();
        if (next == null) {
            return false;
        }
        event.fields.clear();
        event.fields.putAll(next.fields);
        event.windowHandle(next.windowHandle());
        next.close();
        return true;
    }

    public static void SDL_FlushEvents(int typeMin, int typeMax) {
        QUEUE.removeIf(e -> e.type() >= typeMin && e.type() <= typeMax);
    }

    public static long SDL_GetWindowFromEvent(SDL_Event event) {
        return event.windowHandle();
    }

    public static int pending() {
        return QUEUE.size();
    }
}