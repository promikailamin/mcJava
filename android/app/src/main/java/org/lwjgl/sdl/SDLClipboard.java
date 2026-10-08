package org.lwjgl.sdl;

/** Clipboard access bridged to the Android clipboard service. */
public final class SDLClipboard {

    private static volatile java.util.function.Supplier<String> reader = () -> "";
    private static volatile java.util.function.Consumer<String> writer = s -> { };

    private SDLClipboard() {
    }

    public static void install(java.util.function.Supplier<String> read, java.util.function.Consumer<String> write) {
        if (read != null) reader = read;
        if (write != null) writer = write;
    }

    public static String SDL_GetClipboardText() {
        String text = reader.get();
        return text == null ? "" : text;
    }

    public static boolean SDL_SetClipboardText(String text) {
        try {
            writer.accept(text == null ? "" : text);
            return true;
        } catch (RuntimeException e) {
            SDLError.setError(e.getMessage());
            return false;
        }
    }

    public static boolean SDL_HasClipboardText() {
        return !SDL_GetClipboardText().isEmpty();
    }
}