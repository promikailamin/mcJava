package org.lwjgl.sdl;

import org.lwjgl.system.SharedLibrary;

/** Root {@code SDL} bindings surface. The whole layer is emulated, so "loading" always succeeds. */
public final class SDL {

    private static final SharedLibrary LIBRARY = new SharedLibrary() { };

    private SDL() {
    }

    public static SharedLibrary getLibrary() {
        return LIBRARY;
    }
}