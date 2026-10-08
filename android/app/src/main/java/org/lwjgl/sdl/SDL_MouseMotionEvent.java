package org.lwjgl.sdl;

public final class SDL_MouseMotionEvent extends sdl_struct {

    public SDL_MouseMotionEvent(java.util.Map<String, Object> fields, String prefix) {
        super(fields, prefix);
    }

    public static SDL_MouseMotionEvent malloc() { return new SDL_MouseMotionEvent(newMap(), ""); }

    @Override
    public long sizeof() { return 40; }

    public double x() { return double_("x"); }
    public double y() { return double_("y"); }
    public double xrel() { return double_("xrel"); }
    public double yrel() { return double_("yrel"); }

    public SDL_MouseMotionEvent x(double v) { put_("x", v); return this; }
    public SDL_MouseMotionEvent y(double v) { put_("y", v); return this; }
    public SDL_MouseMotionEvent xrel(double v) { put_("xrel", v); return this; }
    public SDL_MouseMotionEvent yrel(double v) { put_("yrel", v); return this; }
}