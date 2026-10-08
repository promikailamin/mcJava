package org.lwjgl.sdl;

public final class SDL_KeyboardEvent extends sdl_struct {

    public SDL_KeyboardEvent(java.util.Map<String, Object> fields, String prefix) {
        super(fields, prefix);
    }

    public static SDL_KeyboardEvent malloc() { return new SDL_KeyboardEvent(newMap(), ""); }

    @Override
    public long sizeof() { return 32; }

    public int scancode() { return int_("scancode"); }
    public int key() { return int_("key"); }
    public int mod() { return int_("mod"); }
    public boolean repeat() { return fields.containsKey(prefix + "repeat") ? int_("repeat") != 0 : false; }

    public SDL_KeyboardEvent scancode(int v) { put_("scancode", v); return this; }
    public SDL_KeyboardEvent key(int v) { put_("key", v); return this; }
    public SDL_KeyboardEvent mod(int v) { put_("mod", v); return this; }
    public SDL_KeyboardEvent repeat(boolean v) { put_("repeat", v ? 1 : 0); return this; }
}