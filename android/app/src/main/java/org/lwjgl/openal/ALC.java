package org.lwjgl.openal;

import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.Library;

/** ALC entry surface: capability + function-provider stubs the game's bootstrap needs. */
public final class ALC {

    private static final FunctionProvider PROVIDER = name -> Library.functionHandle(name);

    private static final long DEFAULT_DEVICE = 0x6001L;

    private ALC() {
    }

    public static FunctionProvider getFunctionProvider() {
        return PROVIDER;
    }

    public static ALCCapabilities createCapabilities(long device) {
        return new ALCCapabilities();
    }

    public static long getDefaultDevice() {
        return DEFAULT_DEVICE;
    }
}