package org.lwjgl.sdl;

public final class SDLMisc {

    private SDLMisc() {
    }

    public static boolean SDL_OpenURL(String url) {
        if (url == null) {
            return false;
        }
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            pro.minecraft.main_activity.instance().startActivity(intent);
            return true;
        } catch (RuntimeException e) {
            SDLError.setError(e.getMessage());
            return false;
        }
    }
}