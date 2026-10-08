package org.lwjgl.sdl;

import org.lwjgl.system.MemoryStack;

/** SDL3 events structs: {@code SDL_DisplayMode}, {@code SDL_Rect}, {@code SDL_PixelFormatDetails}. */
public final class SDL_Rect extends sdl_struct {

    public SDL_Rect() {
        super(newMap(), "");
    }

    public SDL_Rect(java.util.Map<String, Object> fields, String prefix) {
        super(fields, prefix);
    }

    public static SDL_Rect malloc() {
        return new SDL_Rect();
    }

    public static SDL_Rect calloc() {
        return new SDL_Rect();
    }

    public static SDL_Rect malloc(MemoryStack stack) {
        return new SDL_Rect();
    }

    public static Buffer malloc(int capacity, MemoryStack stack) {
        return new Buffer(capacity);
    }

    public static SDL_Rect create(long address) {
        return new SDL_Rect();
    }

    @Override
    public long sizeof() {
        return 16;
    }

    public int x() { return int_("x"); }
    public int y() { return int_("y"); }
    public int w() { return int_("w"); }
    public int h() { return int_("h"); }

    public SDL_Rect x(int v) { return (SDL_Rect) put_("x", v); }
    public SDL_Rect y(int v) { return (SDL_Rect) put_("y", v); }
    public SDL_Rect w(int v) { return (SDL_Rect) put_("w", v); }
    public SDL_Rect h(int v) { return (SDL_Rect) put_("h", v); }

    public static final class Buffer extends sdl_buffer<SDL_Rect> {
        Buffer(int capacity) {
            super(capacity);
        }

        @Override
        protected SDL_Rect newElement() {
            return new SDL_Rect();
        }

        public Buffer x(int v) { current().x(v); return this; }
        public Buffer y(int v) { current().y(v); return this; }
        public Buffer w(int v) { current().w(v); return this; }
        public Buffer h(int v) { current().h(v); return this; }
    }
}