package org.lwjgl.util.vma;

/** Minimal {@code VMA} surface; only teardown is invoked outside the (excluded) Vulkan backend. */
public final class Vma {

    private Vma() {
    }

    public static void vmaDestroyAllocator(long allocator) {
    }

    public static void vmaFreeMemory(long allocation) {
    }
}