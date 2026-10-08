package org.lwjgl.sdl;

/** SDL3 event-type constants and typed event views. */
public final class SDL_Event extends sdl_struct {

    // Event types (subset used by the game)
    public static final int SDL_EVENT_QUIT = 0x100;
    public static final int SDL_EVENT_KEY_DOWN = 0x300;
    public static final int SDL_EVENT_KEY_UP = 0x301;
    public static final int SDL_EVENT_TEXT_EDITING = 0x302;
    public static final int SDL_EVENT_TEXT_INPUT = 0x303;
    public static final int SDL_EVENT_MOUSE_MOTION = 0x400;
    public static final int SDL_EVENT_MOUSE_BUTTON_DOWN = 0x401;
    public static final int SDL_EVENT_MOUSE_BUTTON_UP = 0x402;
    public static final int SDL_EVENT_MOUSE_WHEEL = 0x403;
    public static final int SDL_EVENT_DROP_FILE = 0x1000;
    public static final int SDL_EVENT_DROP_BEGIN = 0x1002;
    public static final int SDL_EVENT_DROP_COMPLETE = 0x1003;

    private final SDL_KeyboardEvent key = new SDL_KeyboardEvent(fields, "key.");
    private final SDL_MouseMotionEvent motion = new SDL_MouseMotionEvent(fields, "motion.");
    private final SDL_MouseButtonEvent button = new SDL_MouseButtonEvent(fields, "button.");
    private final SDL_MouseWheelEvent wheel = new SDL_MouseWheelEvent(fields, "wheel.");
    private final sdl_text_event text = new sdl_text_event(fields, "text.");
    private final SDL_TextEditingEvent edit = new SDL_TextEditingEvent(fields, "edit.");
    private final sdl_text_event drop = new sdl_text_event(fields, "drop.");

    private long window;

    public SDL_Event() {
        super(newMap(), "");
    }

    public static SDL_Event malloc() {
        return new SDL_Event();
    }

    public static SDL_Event calloc() {
        return new SDL_Event();
    }

    @Override
    public long sizeof() {
        return 128;
    }

    public int type() { return int_("type"); }

    public SDL_Event type(int v) { put_("type", v); return this; }

    public SDL_KeyboardEvent key() { return key; }
    public SDL_MouseMotionEvent motion() { return motion; }
    public SDL_MouseButtonEvent button() { return button; }
    public SDL_MouseWheelEvent wheel() { return wheel; }
    public sdl_text_event text() { return text; }
    public SDL_TextEditingEvent edit() { return edit; }
    public sdl_text_event drop() { return drop; }

    public long windowHandle() { return window; }
    public SDL_Event windowHandle(long w) { this.window = w; return this; }
}