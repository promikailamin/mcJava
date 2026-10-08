package org.lwjgl.sdl;

import org.lwjgl.system.MemoryStack;

public final class SDL_DisplayMode extends sdl_struct {

    public SDL_DisplayMode() {
        super(newMap(), "");
    }

    public SDL_DisplayMode(java.util.Map<String, Object> fields, String prefix) {
        super(fields, prefix);
    }

    public static SDL_DisplayMode malloc() {
        return new SDL_DisplayMode();
    }

    public static SDL_DisplayMode calloc() {
        return new SDL_DisplayMode();
    }

    public static SDL_DisplayMode malloc(MemoryStack stack) {
        return new SDL_DisplayMode();
    }

    public static SDL_DisplayMode create(long address) {
        SDL_DisplayMode mode = new SDL_DisplayMode();
        mode.w(SDLVideo.getSurfaceWidth()).h(SDLVideo.getSurfaceHeight()).refresh_rate(60).format(376840196);
        return mode;
    }

    @Override
    public long sizeof() {
        return 40;
    }

    public int format() { return int_("format"); }
    public int w() { return int_("w"); }
    public int h() { return int_("h"); }
    public int refresh_rate() { return int_("refresh_rate"); }
    public int refreshRate() { return refresh_rate(); }

    public SDL_DisplayMode format(int v) { return (SDL_DisplayMode) put_("format", v); }
    public SDL_DisplayMode w(int v) { return (SDL_DisplayMode) put_("w", v); }
    public SDL_DisplayMode h(int v) { return (SDL_DisplayMode) put_("h", v); }
    public SDL_DisplayMode refresh_rate(int v) { return (SDL_DisplayMode) put_("refresh_rate", v); }
    public SDL_DisplayMode refreshRate(int v) { return refresh_rate(v); }
}