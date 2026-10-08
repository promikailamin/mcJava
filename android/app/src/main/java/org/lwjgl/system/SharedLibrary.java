package org.lwjgl.system;

/** Abstraction over the "dynamic library" LWJGL would dlopen. On Android this is always the game itself. */
public interface SharedLibrary {

    default String getPath() {
        return "android";
    }

    default String getName() {
        return "android";
    }

    default long getFunctionAddress(CharSequence name) {
        return Library.functionHandle(name);
    }
}