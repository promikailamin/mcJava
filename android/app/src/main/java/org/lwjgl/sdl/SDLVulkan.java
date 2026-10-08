package org.lwjgl.sdl;

/** Vulkan surface — always reports unsupported so the game never attempts the Vulkan backend. */
public final class SDLVulkan {

    private SDLVulkan() {
    }

    public static boolean SDL_Vulkan_LoadLibrary(String path) {
        SDLError.setError("Vulkan not supported on this port");
        return false;
    }

    public static void SDL_Vulkan_UnloadLibrary() {
    }
}