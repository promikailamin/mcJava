package org.lwjgl.sdl;

/** SDL3 opaque surface handle wrapper (mirrors {@code SDL_Surface*}). */
public final class SDL_Surface extends sdl_struct {

    private final long handle;
    private int width;
    private int height;

    public SDL_Surface(long handle, int width, int height) {
        super(newMap(), "");
        this.handle = handle;
        this.width = width;
        this.height = height;
    }

    public static SDL_Surface create() {
        return new SDL_Surface(SDLSurface.nextSurface(), 0, 0);
    }

    @Override
    public long sizeof() {
        return 8;
    }

    public long address() {
        return handle;
    }

    public long handle() {
        return handle;
    }

    public int w() { return width; }
    public int h() { return height; }
    public void setSize(int w, int h) { this.width = w; this.height = h; }
}