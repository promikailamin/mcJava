package org.lwjgl.util.freetype;

import org.lwjgl.system.MemoryStack;

/** FreeType face handle. Metrics are zeroed by the shim (no glyph rasterization). */
public final class FT_Face {

    public long handle;

    private FT_Face(long handle) {
        this.handle = handle;
    }

    public static FT_Face create(long address) {
        return new FT_Face(address);
    }

    public static FT_Face malloc(MemoryStack stack) {
        return new FT_Face(0L);
    }

    public FT_GlyphSlot glyph() {
        return FT_GlyphSlot.create(handle + 0x20);
    }

    public long num_glyphs() {
        return 0L;
    }

    public long face_flags() {
        return FreeType.FT_FACE_FLAG_SCALABLE;
    }

    public long style_flags() {
        return 0L;
    }

    public long size() {
        return 0L;
    }

    public long address() {
        return handle;
    }
}