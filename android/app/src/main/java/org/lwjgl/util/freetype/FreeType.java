package org.lwjgl.util.freetype;

import java.nio.ByteBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.SharedLibrary;

/**
 * FreeType bindings shim. Loading succeeds (the bootstrap requires a non-null library);
 * glyph rasterization returns empty bitmaps so the TrueType font provider degrades
 * gracefully. A real {@code android.graphics.Typeface}-backed rasterizer is the planned
 * replacement.
 */
public final class FreeType {

    private static final SharedLibrary LIBRARY = new SharedLibrary() { };

    private FreeType() {
    }

    public static SharedLibrary getLibrary() {
        return LIBRARY;
    }

    // ---------------------------------------------------------------- init/teardown

    public static int FT_Init_FreeType(PointerBuffer library) {
        if (library != null) {
            library.put(0, 0x1000001L);
        }
        return 0;
    }

    public static int FT_Done_Library(long library) {
        return 0;
    }

    public static int FT_New_Memory_Face(long library, ByteBuffer file_base, long face_index, PointerBuffer face) {
        if (face != null) {
            face.put(0, 0x1000002L);
        }
        return 0;
    }

    public static int FT_Done_Face(FT_Face face) {
        return 0;
    }

    // ---------------------------------------------------------------- configuration

    public static int FT_Set_Pixel_Sizes(FT_Face face, int pixel_width, int pixel_height) {
        return 0;
    }

    public static int FT_Set_Transform(FT_Face face, Object matrix, FT_Vector delta) {
        return 0;
    }

    public static int FT_Select_Charmap(FT_Face face, int encoding) {
        return encoding == 0 ? 0 : 0x06;
    }

    public static int FT_Load_Glyph(FT_Face face, int glyph_index, int load_flags) {
        return face == null ? 0x23 : 0;
    }

    public static ByteBuffer FT_Get_Font_Format(FT_Face face) {
        return ByteBuffer.wrap("opencode".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    // ---------------------------------------------------------------- charset walking

    public static long FT_Get_First_Char(FT_Face face, java.nio.IntBuffer charcode_offset) {
        if (charcode_offset != null) {
            charcode_offset.put(0, 0);
        }
        return 0L;
    }

    public static long FT_Get_Next_Char(FT_Face face, long charcode, java.nio.IntBuffer charcode_offset) {
        if (charcode_offset != null) {
            charcode_offset.put(0, 0);
        }
        return 0L;
    }

    // ---------------------------------------------------------------- misc

    public static String FT_Error_String(int code) {
        return code == 0 ? "No error" : "error 0x" + Integer.toHexString(code);
    }

    public static final int FT_ENCODING_UNICODE = 0x6d616e78;
    public static final int FT_LOAD_RENDER = 0x4;
    public static final int FT_LOAD_NO_BITMAP = 0x8;
    public static final int FT_LOAD_TARGET_NORMAL = 0;
    public static final int FT_FACE_FLAG_SCALABLE = 1 << 0;
    public static final int FT_FACE_FLAG_FIXED_SIZES = 1 << 1;
    public static final int FT_STYLE_FLAG_ITALIC = 1 << 0;
    public static final int FT_STYLE_FLAG_BOLD = 1 << 1;
    public static final int FT_PIXEL_MODE_GRAY = 2;
}