package org.lwjgl.opengl;

import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.Library;
import org.lwjgl.system.SharedLibrary;

/** LWJGL {@code GL} facade: capabilities + function provider backed by the Android shim. */
public final class GL {

    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;

    private static final FunctionProvider PROVIDER = new GLProvider();

    private static final class GLProvider implements SharedLibrary, FunctionProvider {
        @Override
        public long getFunctionAddress(CharSequence functionName) {
            return Library.functionHandle(functionName);
        }
    }

    private static GLCapabilities capabilities;
    private static Object currentThread = new Object();

    private GL() {
    }

    public static GLCapabilities createCapabilities() {
        return getCapabilities();
    }

    public static synchronized GLCapabilities getCapabilities() {
        if (capabilities == null) {
            capabilities = new GLCapabilities();
        }
        return capabilities;
    }

    public static FunctionProvider getFunctionProvider() {
        return PROVIDER;
    }

    public static void setCapabilities(GLCapabilities caps) {
        capabilities = caps;
    }
}