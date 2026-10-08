package com.android;

import java.util.HashMap;
import java.util.Map;

import android.view.KeyEvent;

/**
 * Maps Android {@code KeyEvent} key codes to SDL3 scancodes/keycodes. SDL3 reuses the
 * same values for scancodes and keycodes (SDL_SCANCODE_A == SDLK_A == 4), so the game's
 * HID-driven input handling works unchanged. SDL key names mirror SDL's table.
 */
public final class keymap {

    private static final Map<Integer, Integer> ANDROID_TO_SDL = new HashMap<>();
    private static final Map<Integer, String> SDL_NAMES = new HashMap<>();

    static {
        put(KeyEvent.KEYCODE_A, 4, "a");
        put(KeyEvent.KEYCODE_B, 5, "b");
        put(KeyEvent.KEYCODE_C, 6, "c");
        put(KeyEvent.KEYCODE_D, 7, "d");
        put(KeyEvent.KEYCODE_E, 8, "e");
        put(KeyEvent.KEYCODE_F, 9, "f");
        put(KeyEvent.KEYCODE_G, 10, "g");
        put(KeyEvent.KEYCODE_H, 11, "h");
        put(KeyEvent.KEYCODE_I, 12, "i");
        put(KeyEvent.KEYCODE_J, 13, "j");
        put(KeyEvent.KEYCODE_K, 14, "k");
        put(KeyEvent.KEYCODE_L, 15, "l");
        put(KeyEvent.KEYCODE_M, 16, "m");
        put(KeyEvent.KEYCODE_N, 17, "n");
        put(KeyEvent.KEYCODE_O, 18, "o");
        put(KeyEvent.KEYCODE_P, 19, "p");
        put(KeyEvent.KEYCODE_Q, 20, "q");
        put(KeyEvent.KEYCODE_R, 21, "r");
        put(KeyEvent.KEYCODE_S, 22, "s");
        put(KeyEvent.KEYCODE_T, 23, "t");
        put(KeyEvent.KEYCODE_U, 24, "u");
        put(KeyEvent.KEYCODE_V, 25, "v");
        put(KeyEvent.KEYCODE_W, 26, "w");
        put(KeyEvent.KEYCODE_X, 27, "x");
        put(KeyEvent.KEYCODE_Y, 28, "y");
        put(KeyEvent.KEYCODE_Z, 29, "z");
        put(KeyEvent.KEYCODE_0, 39, "0");
        put(KeyEvent.KEYCODE_1, 30, "1");
        put(KeyEvent.KEYCODE_2, 31, "2");
        put(KeyEvent.KEYCODE_3, 32, "3");
        put(KeyEvent.KEYCODE_4, 33, "4");
        put(KeyEvent.KEYCODE_5, 34, "5");
        put(KeyEvent.KEYCODE_6, 35, "6");
        put(KeyEvent.KEYCODE_7, 36, "7");
        put(KeyEvent.KEYCODE_8, 37, "8");
        put(KeyEvent.KEYCODE_9, 38, "9");
        put(KeyEvent.KEYCODE_ENTER, 40, "Return");
        put(KeyEvent.KEYCODE_ESCAPE, 41, "Escape");
        put(KeyEvent.KEYCODE_DEL, 42, "Backspace");
        put(KeyEvent.KEYCODE_TAB, 43, "Tab");
        put(KeyEvent.KEYCODE_SPACE, 44, "Space");
        put(KeyEvent.KEYCODE_MINUS, 45, "-");
        put(KeyEvent.KEYCODE_EQUALS, 46, "=");
        put(KeyEvent.KEYCODE_LEFT_BRACKET, 47, "[");
        put(KeyEvent.KEYCODE_RIGHT_BRACKET, 48, "]");
        put(KeyEvent.KEYCODE_BACKSLASH, 49, "\\");
        put(KeyEvent.KEYCODE_SEMICOLON, 51, ";");
        put(KeyEvent.KEYCODE_APOSTROPHE, 52, "'");
        put(KeyEvent.KEYCODE_GRAVE, 53, "`");
        put(KeyEvent.KEYCODE_COMMA, 54, ",");
        put(KeyEvent.KEYCODE_PERIOD, 55, ".");
        put(KeyEvent.KEYCODE_SLASH, 56, "/");
        put(KeyEvent.KEYCODE_CAPS_LOCK, 57, "Caps Lock");
        put(KeyEvent.KEYCODE_F1, 58, "F1");
        put(KeyEvent.KEYCODE_F2, 59, "F2");
        put(KeyEvent.KEYCODE_F3, 60, "F3");
        put(KeyEvent.KEYCODE_F4, 61, "F4");
        put(KeyEvent.KEYCODE_F5, 62, "F5");
        put(KeyEvent.KEYCODE_F6, 63, "F6");
        put(KeyEvent.KEYCODE_F7, 64, "F7");
        put(KeyEvent.KEYCODE_F8, 65, "F8");
        put(KeyEvent.KEYCODE_F9, 66, "F9");
        put(KeyEvent.KEYCODE_F10, 67, "F10");
        put(KeyEvent.KEYCODE_F11, 68, "F11");
        put(KeyEvent.KEYCODE_F12, 69, "F12");
        put(KeyEvent.KEYCODE_INSERT, 73, "Insert");
        put(KeyEvent.KEYCODE_HOME, 74, "Home");
        put(KeyEvent.KEYCODE_PAGE_UP, 75, "Page Up");
        put(KeyEvent.KEYCODE_DEL, 76, "Delete");
        put(KeyEvent.KEYCODE_END, 77, "End");
        put(KeyEvent.KEYCODE_PAGE_DOWN, 78, "Page Down");
        put(KeyEvent.KEYCODE_DPAD_RIGHT, 79, "Right");
        put(KeyEvent.KEYCODE_DPAD_LEFT, 80, "Left");
        put(KeyEvent.KEYCODE_DPAD_DOWN, 81, "Down");
        put(KeyEvent.KEYCODE_DPAD_UP, 82, "Up");
        put(KeyEvent.KEYCODE_NUM_LOCK, 83, "Num Lock");
        put(KeyEvent.KEYCODE_NUMPAD_DIVIDE, 84, "Keypad /");
        put(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, 85, "Keypad *");
        put(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, 86, "Keypad -");
        put(KeyEvent.KEYCODE_NUMPAD_ADD, 87, "Keypad +");
        put(KeyEvent.KEYCODE_NUMPAD_ENTER, 88, "Keypad Enter");
        put(KeyEvent.KEYCODE_NUMPAD_1, 89, "Keypad 1");
        put(KeyEvent.KEYCODE_NUMPAD_2, 90, "Keypad 2");
        put(KeyEvent.KEYCODE_NUMPAD_3, 91, "Keypad 3");
        put(KeyEvent.KEYCODE_NUMPAD_4, 92, "Keypad 4");
        put(KeyEvent.KEYCODE_NUMPAD_5, 93, "Keypad 5");
        put(KeyEvent.KEYCODE_NUMPAD_6, 94, "Keypad 6");
        put(KeyEvent.KEYCODE_NUMPAD_7, 95, "Keypad 7");
        put(KeyEvent.KEYCODE_NUMPAD_8, 96, "Keypad 8");
        put(KeyEvent.KEYCODE_NUMPAD_9, 97, "Keypad 9");
        put(KeyEvent.KEYCODE_NUMPAD_DOT, 99, "Keypad .");
        put(KeyEvent.KEYCODE_SHIFT_LEFT, 225, "Left Shift");
        put(KeyEvent.KEYCODE_SHIFT_RIGHT, 225, "Right Shift");
        put(KeyEvent.KEYCODE_CTRL_LEFT, 224, "Left Control");
        put(KeyEvent.KEYCODE_CTRL_RIGHT, 224, "Right Control");
        put(KeyEvent.KEYCODE_ALT_LEFT, 226, "Left Alt");
        put(KeyEvent.KEYCODE_ALT_RIGHT, 226, "Right Alt");
        put(KeyEvent.KEYCODE_META_LEFT, 227, "Left GUI");
        put(KeyEvent.KEYCODE_META_RIGHT, 227, "Right GUI");
    }

    private static void put(int androidCode, int sdlScancode, String name) {
        ANDROID_TO_SDL.put(androidCode, sdlScancode);
        SDL_NAMES.put(sdlScancode, name);
    }

    private keymap() {
    }

    /** SDL3 keeps keycodes equal to scancodes. */
    public static int scancode_to_key(int scancode) {
        return scancode;
    }

    public static String key_name(int keycode) {
        return SDL_NAMES.getOrDefault(keycode, Integer.toString(keycode));
    }

    public static int android_to_scancode(int keyCode) {
        return ANDROID_TO_SDL.getOrDefault(keyCode, 0);
    }

    /** Translate a {@code KeyEvent} meta-state into SDL modifier bits. */
    public static int android_to_mods(int metaState) {
        int mods = 0;
        if ((metaState & KeyEvent.META_SHIFT_ON) != 0) mods |= 0x1;
        if ((metaState & KeyEvent.META_CTRL_ON) != 0) mods |= 0x2;
        if ((metaState & KeyEvent.META_ALT_ON) != 0) mods |= 0x4;
        if ((metaState & KeyEvent.META_META_ON) != 0) mods |= 0x8;
        return mods;
    }

    public static boolean is_modifier(int scancode) {
        return scancode >= 224 && scancode <= 231;
    }
}