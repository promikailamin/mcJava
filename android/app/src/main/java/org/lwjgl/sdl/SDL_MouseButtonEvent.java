package org.lwjgl.sdl;

public final class SDL_MouseButtonEvent extends sdl_struct {

    public SDL_MouseButtonEvent(java.util.Map<String, Object> fields, String prefix) {
        super(fields, prefix);
    }

    public static SDL_MouseButtonEvent malloc() { return new SDL_MouseButtonEvent(newMap(), ""); }

    @Override
    public long sizeof() { return 32; }

    public int button() { return int_("button"); }
    public int clicks() { return int_("clicks"); }

    public SDL_MouseButtonEvent button(int v) { put_("button", v); return this; }
    public SDL_MouseButtonEvent clicks(int v) { put_("clicks", v); return this; }
}