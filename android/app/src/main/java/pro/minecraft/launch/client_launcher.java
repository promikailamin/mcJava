package pro.minecraft.launch;

import java.io.File;
import java.net.Proxy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.util.Log;

import com.mojang.blaze3d.TracyBootstrap;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.TracyClient;

import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PreferredGraphicsApi;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;

import org.lwjgl.sdl.SDLVideo;

import pro.minecraft.main_activity;
import pro.minecraft.host.game_renderer;

/**
 * Boots the game on a dedicated engine thread (the thread that owns the OpenGL ES context),
 * mirroring {@code net.minecraft.client.main.Main} without the joptsimple CLI layer.
 */
public final class client_launcher {

    private static final String TAG = "client_launcher";

    private final main_activity activity;
    private final game_renderer renderer;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean shutdown = new AtomicBoolean();

    private Thread engineThread;
    private Minecraft minecraft;

    public client_launcher(main_activity activity, game_renderer renderer) {
        this.activity = activity;
        this.renderer = renderer;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        engineThread = new Thread(this::runEngine, "Engine Thread");
        engineThread.setUncaughtExceptionHandler((t, e) -> Log.e(TAG, "engine died", e));
        engineThread.start();
    }

    public void shutdown() {
        shutdown.set(true);
        if (minecraft != null) {
            try {
                minecraft.stop();
            } catch (Throwable ignored) {
            }
        }
    }

    private void runEngine() {
        try {
            if (!renderer.create_context()) {
                Log.e(TAG, "EGL context creation failed");
                return;
            }
            SDLVideo.set_swap_function(() -> renderer.swap());
            SDLVideo.updateDisplay(renderer_surface_width(), renderer_surface_height(), density());
            boot();
        } catch (Throwable t) {
            Log.e(TAG, "game boot crashed", t);
            t.printStackTrace();
        }
    }

    private void boot() throws Exception {
        SharedConstants.tryDetectVersion();
        NativeLibrariesBootstrap.loadLibraries();

        File gameDir = activity.getGameDir();
        File resourcePacks = new File(gameDir, "resourcepacks");
        File assetsDir = new File(activity.getAssetsDir());

        Logger_progress("Extracting assets from APK assets …");
        String assetIndex = asset_provider.extract(activity, gameDir);
        if (assetIndex == null) {
            assetIndex = "34";
        }
        Log.i(TAG, "asset index: " + assetIndex);

        TracyBootstrap.setup();
        TracyClient.reportAppInfo("Minecraft Java Edition " + SharedConstants.getCurrentVersion().name());
        CompletableFuture<?> dataFixerOptimization = DataFixers.optimize(DataFixTypes.TYPES_FOR_LEVEL_LIST);

        Bootstrap.bootStrap();
        ClientBootstrap.bootstrap();

        Thread shutdownThread = new Thread(() -> {
            Minecraft instance = Minecraft.getInstance();
            if (instance != null && instance.getSingleplayerServer() != null) {
                instance.getSingleplayerServer().halt(true);
            }
        }, "Client Shutdown Thread");
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        int width = Math.max(1, renderer_surface_width());
        int height = Math.max(1, renderer_surface_height());
        String username = "Player" + (System.currentTimeMillis() % 1000L);
        UUID uuid = UUIDUtil.createOfflinePlayerUUID(username);
        User user = new User(username, uuid, "0", Optional.empty(), Optional.empty());

        GameConfig config = new GameConfig(
            new GameConfig.UserData(user, Proxy.NO_PROXY),
            new DisplayData(width, height, java.util.OptionalInt.of(width), java.util.OptionalInt.of(height), true),
            new GameConfig.FolderData(gameDir, resourcePacks, assetsDir, assetIndex),
            new GameConfig.GameData(
                false,
                SharedConstants.getCurrentVersion().getName(),
                "release",
                false,
                false,
                false,
                false,
                false,
                null,
                true
            ),
            new GameConfig.QuickPlayData(null, GameConfig.QuickPlayVariant.DISABLED)
        );

        dataFixerOptimization.join();

        Thread.currentThread().setName("Render thread");
        RenderSystem.initRenderThread();
        minecraft = new Minecraft(config);
        minecraft.run();
        try {
            minecraft.exitWorldAndClose();
        } catch (Throwable ignored) {
        }
    }

    private int renderer_surface_width() {
        return org.lwjgl.sdl.SDLVideo.getSurfaceWidth();
    }

    private int renderer_surface_height() {
        return org.lwjgl.sdl.SDLVideo.getSurfaceHeight();
    }

    private float density() {
        return activity.getResources().getDisplayMetrics().density;
    }

    private void Logger_progress(String msg) {
        Log.i(TAG, msg);
    }

    /** Small progress surface for UI/README clarity (kept snake_case for the app package). */
    public interface progress_listener {
        void on_progress(String message);
    }
}