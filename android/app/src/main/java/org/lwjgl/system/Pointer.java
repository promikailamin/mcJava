package org.lwjgl.system;

/** LWJGL {@code Pointer}: an opaque native address handle. */
public interface Pointer {

    long address();

    default long address0() {
        return address();
    }
}