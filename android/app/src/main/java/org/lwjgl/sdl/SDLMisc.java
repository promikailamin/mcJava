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
            pro.minecraft.main_activity activity = pro.minecraft.main_activity.instance();
            if (activity != null) {
                activity.startActivity(intent);
                return true;
            }
            return false;
        } catch (RuntimeException e) {
            SDLError.setError(e.getMessage());
            return false;
        }
    }
}