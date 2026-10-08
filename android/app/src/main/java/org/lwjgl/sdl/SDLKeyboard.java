package org.lwjgl.sdl;

import java.nio.ByteBuffer;

import com.android.keymap;

/**
 * Keyboard surface backed by Android key events. Scancodes use SDL's layout so the
 * game's HID/input code works unchanged; keycodes are the SDL3 {@code SDLK_} values.
 */
public final class SDLKeyboard {

    private static final byte[] STATE = new byte[512];
    private static final java.util.concurrent.atomic.AtomicInteger MOD_STATE = new java.util.concurrent.atomic.AtomicInteger();

    public static final int KMOD_NONE = 0;
    public static final int KMOD_SHIFT = 0x1;
    public static final int KMOD_CTRL = 0x2;
    public static final int KMOD_ALT = 0x4;
    public static final int KMOD_GUI = 0x8;
    public static final int KMOD_CAPS = 0x10;

    private SDLKeyboard() {
    }

    public static ByteBuffer SDL_GetKeyboardState(int[] numkeys) {
        if (numkeys != null && numkeys.length > 0) {
            numkeys[0] = STATE.length;
        }
        return ByteBuffer.wrap(STATE);
    }

    public static ByteBuffer SDL_GetKeyboardState() {
        return ByteBuffer.wrap(STATE);
    }

    public static int SDL_GetModState() {
        return MOD_STATE.get();
    }

    public static void setModState(int mods) {
        MOD_STATE.set(mods);
    }

    public static int SDL_GetKeyFromScancode(int scancode, int modState, boolean ignoreModState) {
        return keymap.scancode_to_key(scancode);
    }

    public static String SDL_GetKeyName(int keycode) {
        return keymap.key_name(keycode);
    }

    public static boolean SDL_StartTextInput() { return true; }
    public static boolean SDL_StopTextInput() { return true; }
    public static boolean SDL_ClearComposition() { return true; }

    public static boolean SDL_SetTextInputArea(long window, SDL_Rect.Buffer area, int inputX) {
        return true;
    }

    public static boolean SDL_SetTextInputArea(long window, SDL_Rect area, int inputX) {
        return true;
    }

    /** Press/release a scancode (SDL layout) from the Android event bridge. */
    public static void setScancode(int scancode, boolean down) {
        if (scancode >= 0 && scancode < STATE.length) {
            STATE[scancode] = (byte) (down ? 1 : 0);
        }
    }
}