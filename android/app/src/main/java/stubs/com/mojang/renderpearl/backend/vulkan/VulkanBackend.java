package com.mojang.renderpearl.backend.vulkan;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import org.jspecify.annotations.Nullable;

/**
 * Android stub: the Vulkan backend is not shipped on this platform, so it always
 * reports unavailable and the launcher falls back to the OpenGL backend.
 */
public class VulkanBackend implements GpuBackend {

   private static final String UNAVAILABLE = "Vulkan backend is unavailable on Android";

   public static @Nullable BackendCreationException checkBackendAvailable() {
      return new BackendCreationException(UNAVAILABLE, BackendCreationException.Reason.VULKAN_LOADER_MISSING);
   }

   @Override
   public String getName() {
      return "Vulkan (stub)";
   }

   @Override
   public void loadLibrary() throws BackendCreationException {
      throw new BackendCreationException(UNAVAILABLE, BackendCreationException.Reason.VULKAN_LOADER_MISSING);
   }

   @Override
   public void unloadLibrary() {
   }

   @Override
   public long createWindow(@Nullable String title, int width, int height, long flags) {
      return 0L;
   }

   @Override
   public GpuDevice createDevice(GpuDebugOptions debugOptions) throws BackendCreationException {
      throw new BackendCreationException(UNAVAILABLE, BackendCreationException.Reason.VULKAN_LOADER_MISSING);
   }
}