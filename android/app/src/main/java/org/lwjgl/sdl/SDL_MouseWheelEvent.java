package org.lwjgl.sdl;

public final class SDL_MouseWheelEvent extends sdl_struct {

    public SDL_MouseWheelEvent(java.util.Map<String, Object> fields, String prefix) {
        super(fields, prefix);
    }

    public static SDL_MouseWheelEvent malloc() { return new SDL_MouseWheelEvent(newMap(), ""); }

    @Override
    public long sizeof() { return 40; }

    public double x() { return double_("x"); }
    public double y() { return double_("y"); }

    public SDL_MouseWheelEvent x(double v) { put_("x", v); return this; }
    public SDL_MouseWheelEvent y(double v) { put_("y", v); return this; }
}