package org.lwjgl.sdl;

public final class SDL_PixelFormatDetails extends sdl_struct {

    public SDL_PixelFormatDetails() {
        super(newMap(), "");
    }

    public static SDL_PixelFormatDetails calloc() {
        return new SDL_PixelFormatDetails();
    }

    @Override
    public long sizeof() {
        return 48;
    }

    public int format() { return int_("format"); }
    public int bitsPerPixel() { return int_("bitsPerPixel"); }
    public int Rbits() { return int_("Rbits"); }
    public int Gbits() { return int_("Gbits"); }
    public int Bbits() { return int_("Bbits"); }
    public int Abits() { return int_("Abits"); }
}