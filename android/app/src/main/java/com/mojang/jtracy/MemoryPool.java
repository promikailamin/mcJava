package com.mojang.jtracy;

public final class MemoryPool {

    MemoryPool(String name) {
    }

    public long allocate(long size) {
        return 0L;
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
    }

    public void setName(String name) {
    }
}