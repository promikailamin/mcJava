package org.lwjgl.system;

/** Provider of GL/AL function addresses (LWJGL's {@code FunctionProvider}). */
public interface FunctionProvider {

    long getFunctionAddress(CharSequence functionName);

    default long getFunctionAddress(CharSequence extensionName, CharSequence functionName) {
        return getFunctionAddress(functionName);
    }
}