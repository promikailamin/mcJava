package pro.minecraft.launch;

import android.util.Log;

/**
 * Fabric Loader hook. The decompiled loader sources (net/fabricmc/**) are kept in the
 * sources but excluded from the build until its remaining dependencies (Mixin, TinyRemapper,
 * SpongePowered ASM) are added as Android-JVM libraries.
 *
 * <p>{@link #launchMinecraft(String[])} switches to the vanilla path now and logs the state.
 * When the loader is wired in, this becomes {@code KnotClient.launch(args)} on the engine
 * thread through the Fabric entrypoint contract.</p>
 */
public final class fabric_bootstrap {

    private static final String TAG = "fabric_bootstrap";

    private fabric_bootstrap() {
    }

    public static void launchMinecraft(String[] args) {
        Log.w(TAG, "Fabric Loader 0.19.5 not wired into the Android runtime yet (see README). " +
                "Launching the vanilla client. The loader source (net/fabricmc/**) is in the tree, " +
                "excluded from compilation until Mixin/TinyRemapper/ASM dependencies are shipped.");
    }
}