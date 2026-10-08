package com.mojang.jtracy;

public final class MemoryPool {

    MemoryPool(String name) {
    }

    public long allocate(long size) {
        return org.lwjgl.system.MemoryUtil.nalloc(size);
    }

    public long malloc(long address, long size) {
        return address;
    }

    public long calloc(long address, long size) {
        return address;
    }

    public long realloc(long address, long newSize) {
        return address;
    }

    public void free(long address) {
        if (address != 0L) {
            org.lwjgl.system.MemoryUtil.nfree(address);
        }
    }

    public void setName(String name) {
    }
}