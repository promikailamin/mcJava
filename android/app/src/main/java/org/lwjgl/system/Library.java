package org.lwjgl.system;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Library loading/function resolution surface. Every resolved symbol gets a unique
 * handle; the shim's GL entry points are plain Java methods, so addresses are only
 * used by equality checks in the game's GL bootstrap.
 */
public final class Library {

    private static final AtomicLong HANDLE = new AtomicLong(0x100000L);
    private static final ConcurrentHashMap<String, Long> SYMBOLS = new ConcurrentHashMap<>();

    private Library() {
    }

    public static void initialize() {
    }

    public static SharedLibrary loadSystem(CharSequence name) {
        return new SharedLibrary() { };
    }

    public static SharedLibrary loadNative(CharSequence name) {
        return new SharedLibrary() { };
    }

    public static long functionHandle(CharSequence name) {
        String key = name.toString();
        return SYMBOLS.computeIfAbsent(key, k -> HANDLE.incrementAndGet());
    }
}