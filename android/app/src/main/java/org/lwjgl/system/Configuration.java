package org.lwjgl.system;

/** LWJGL runtime configuration surface (option get/set). */
public final class Configuration {

    private Configuration() {
    }

    public static final class Option<T> {
        private volatile T value;
        private volatile T defaultValue;

        Option() {
        }

        public T get() {
            return value != null ? value : defaultValue;
        }

        public T get(T defaultValue) {
            return value != null ? value : defaultValue;
        }

        public void set(T value) {
            this.value = value;
        }

        public void setDefault(T value) {
            this.defaultValue = value;
        }
    }

    private static <T> Option<T> option() {
        return new Option<>();
    }

    public static final Option<String> SHARED_LIBRARY_EXTRACT_PATH = option();
    public static final Option<String> LIBRARY_PATH = option();
    public static final Option<String> EXTRACT_PATH = option();
    public static final Option<String> DEBUG_STREAM = option();
    public static final Option<Boolean> DEBUG = option();
    public static final Option<Boolean> DEBUG_LOADER = option();
    public static final Option<Boolean> DEBUG_CHECKS = option();
    public static final Option<Boolean> DEBUG_STACK = option();
    public static final Option<Boolean> VULKAN_EXPLICIT_INIT = option();
    public static final Option<String> OPENAL_EXPLICIT_INIT = option();
    public static final Option<Boolean> GLFW_CHECK_THREAD0 = option();
    public static final Option<Integer> JEMALLOC_INIT = option();
    public static final Option<String> STB_INCLUDE_PATH = option();
    public static final Option<Boolean> LIBRARY_MUTABLE = option();
    public static final Option<String> LOADER_PATH = option();
}