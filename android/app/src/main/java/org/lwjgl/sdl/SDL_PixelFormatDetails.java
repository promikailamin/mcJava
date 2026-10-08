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

    public SDL_PixelFormatDetails setFormat(int value) { put_("format", value); return this; }
    public SDL_PixelFormatDetails setBitsPerPixel(int value) { put_("bitsPerPixel", value); return this; }
    public SDL_PixelFormatDetails setRbits(int value) { put_("Rbits", value); return this; }
    public SDL_PixelFormatDetails setGbits(int value) { put_("Gbits", value); return this; }
    public SDL_PixelFormatDetails setBbits(int value) { put_("Bbits", value); return this; }
    public SDL_PixelFormatDetails setAbits(int value) { put_("Abits", value); return this; }
}