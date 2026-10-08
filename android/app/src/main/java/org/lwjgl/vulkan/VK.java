package org.lwjgl.vulkan;

/** Stub Vulkan loader; always unavailable on this port (see NativeLibrariesBootstrap). */
public final class VK {

    private VK() {
    }

    public static void create() {
        throw new UnsupportedOperationException("Vulkan loader unavailable on this port");
    }

    public static void create(FunctionProvider functionProvider, boolean explicitInit) {
        throw new UnsupportedOperationException("Vulkan loader unavailable on this port");
    }

    public interface FunctionProvider {
        long getFunctionAddress(CharSequence name);
    }
}