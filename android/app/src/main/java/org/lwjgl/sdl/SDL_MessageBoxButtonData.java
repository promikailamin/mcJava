package org.lwjgl.sdl;

public final class SDL_MessageBoxButtonData extends sdl_struct {

    public SDL_MessageBoxButtonData() {
        super(newMap(), "");
    }

    public static Buffer calloc(int capacity, org.lwjgl.system.MemoryStack stack) {
        return new Buffer(capacity);
    }

    public static SDL_MessageBoxButtonData calloc(org.lwjgl.system.MemoryStack stack) {
        return new SDL_MessageBoxButtonData();
    }

    @Override
    public long sizeof() {
        return 24;
    }

    public int buttonID() { return int_("buttonID"); }

    public SDL_MessageBoxButtonData buttonID(int v) { put_("buttonID", v); return this; }

    public SDL_MessageBoxButtonData text(long address) { put_("text", address); return this; }

    public int flags() { return int_("flags"); }

    public SDL_MessageBoxButtonData flags(int v) { put_("flags", v); return this; }

    public static final class Buffer extends sdl_buffer<SDL_MessageBoxButtonData> {
        Buffer(int capacity) {
            super(capacity);
        }

        @Override
        protected SDL_MessageBoxButtonData newElement() {
            return new SDL_MessageBoxButtonData();
        }
    }
}