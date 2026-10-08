package org.lwjgl.sdl;

/** Backs {@code SDL_TextInputEvent}, {@code SDL_TextEditingEvent} and {@code SDL_DropEvent}. */
public final class sdl_text_event extends sdl_struct {

    public sdl_text_event(java.util.Map<String, Object> fields, String prefix) {
        super(fields, prefix);
    }

    @Override
    public long sizeof() { return 24; }

    public String textString() { return string_("text"); }
    public String dataString() { return string_("text"); }
    public int start() { return int_("start"); }
    public int length() { return int_("length"); }

    public sdl_text_event text(String v) { put_("text", v); return this; }
    public sdl_text_event start(int v) { put_("start", v); return this; }
    public sdl_text_event length(int v) { put_("length", v); return this; }
}