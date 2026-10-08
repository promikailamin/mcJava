package org.lwjgl.system;

/** LWJGL {@code NativeType} marker for native-struct-backed types. */
public interface NativeType {
    long sizeof();
}