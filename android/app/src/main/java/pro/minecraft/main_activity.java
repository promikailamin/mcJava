package pro.minecraft;

import java.io.File;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;

import org.lwjgl.sdl.SDLClipboard;
import org.lwjgl.sdl.SDLVideo;

import pro.minecraft.host.game_view;
import pro.minecraft.launch.client_launcher;

public final class main_activity extends abstract_fullscreen_activity {

    private static volatile main_activity instance;

    private game_view gameView;
    private client_launcher client;

    public static main_activity instance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        if (clipboard != null) {
            SDLClipboard.install(
                () -> {
                    if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null
                            && clipboard.getPrimaryClip().getItemCount() > 0) {
                        return clipboard.getPrimaryClip().getItemAt(0).coerceToText(main_activity.this).toString();
                    }
                    return "";
                },
                text -> clipboard.setPrimaryClip(ClipData.newPlainText("Minecraft", text))
            );
        }

        gameView = new game_view(this);
        setContentView(gameView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (client == null) {
            client = new client_launcher(this, gameView.renderer());
        }
        client.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        instance = null;
        if (client != null) {
            client.shutdown();
            client = null;
        }
        gameView = null;
        super.onDestroy();
    }

    /** Message box surface for SDL_ShowSimpleMessageBox. */
    public static void showMessageBox(int flags, String title, String message) {
        main_activity activity = instance;
        if (activity == null || activity.isFinishing()) {
            return;
        }
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
            .show());
    }

    public game_view get_game_view() {
        return gameView;
    }

    public File getGameDir() {
        return new File(getFilesDir(), "minecraft");
    }

    public File getAssetsDir() {
        return new File(getFilesDir(), "assets");
    }

    /** Bridges the engine's swap to SDL's vsync pacing bookkeeping (set in client_launcher). */
    public void hook_swap() {
        SDLVideo.set_swap_function(() -> gameView.renderer().swap());
    }
}