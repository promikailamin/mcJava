package org.lwjgl.sdl;

import org.lwjgl.system.MemoryStack;

/** Messaging surfaces ({@code SDLMessageBox} + structs). Android shows dialogs via the host activity. */
public final class SDLMessageBox {

    public static final int SDL_MESSAGEBOX_ERROR = 0x00000010;
    public static final int SDL_MESSAGEBOX_WARNING = 0x00000020;
    public static final int SDL_MESSAGEBOX_INFORMATION = 0x00000040;
    public static final int SDL_MESSAGEBOX_BUTTONS_LEFT_TO_RIGHT = 0x00000100;

    private SDLMessageBox() {
    }

    public static boolean SDL_ShowSimpleMessageBox(int flags, CharSequence title, CharSequence message, long parentWindow) {
        return pro.minecraft.main_activity.showMessageBox(flags, title.toString(), message.toString());
    }

    public static boolean SDL_ShowMessageBox(SDL_MessageBoxData boxdata, java.nio.IntBuffer buttonid) {
        SDL_MessageBoxButtonData defaultButton = boxdata.buttons() != null ? boxdata.buttons().get(0) : null;
        buttonid.put(0, defaultButton != null ? defaultButton.buttonID() : 0);
        return true;
    }
}