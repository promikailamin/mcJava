package org.lwjgl.util.freetype;

/** Glyph slot surface exposed to the game's font renderer. */
public final class FT_GlyphSlot {

    private final long handle;

    private FT_GlyphSlot(long handle) {
        this.handle = handle;
    }

    public static FT_GlyphSlot create(long address) {
        return new FT_GlyphSlot(address);
    }

    public FT_Vector advance() {
        return new FT_Vector();
    }

    public FT_Bitmap bitmap() {
        return new FT_Bitmap();
    }

    public int bitmap_left() {
        return 0;
    }

    public int bitmap_top() {
        return 0;
    }

    public long library() {
        return 0L;
    }

    public long glyph_index() {
        return 0L;
    }
}