package org.lwjgl.sdl;

public final class SDL_MessageBoxData extends sdl_struct {

    public SDL_MessageBoxData() {
        super(newMap(), "");
    }

    public static SDL_MessageBoxData calloc() {
        return new SDL_MessageBoxData();
    }

    public static SDL_MessageBoxData calloc(org.lwjgl.system.MemoryStack stack) {
        return new SDL_MessageBoxData();
    }

    @Override
    public long sizeof() {
        return 48;
    }

    public SDL_MessageBoxData flags(int v) { put_("flags", v); return this; }

    public SDL_MessageBoxData title(long address) { put_("title", address); return this; }

    public SDL_MessageBoxData message(long address) { put_("message", address); return this; }

    public SDL_MessageBoxData buttons(SDL_MessageBoxButtonData.Buffer buttons) { put_("buttons", buttons); return this; }

    public SDL_MessageBoxButtonData.Buffer buttons() {
        Object v = fields.get(prefix + "buttons");
        return v instanceof SDL_MessageBoxButtonData.Buffer b ? b : null;
    }
}