package org.lwjgl.system;

/** Reports the "platform" to the game's native-libraries bootstrap. */
public enum Platform {

    LINUX("Linux", "linux"),
    WINDOWS("Windows", "windows"),
    MACOSX("macOS", "macos"),
    ANDROID("Android", "android"),
    SOLARIS("Solaris", "solaris"),
    FREEBSD("FreeBSD", "freebsd");

    private final String name;
    private final String os;

    Platform(String name, String os) {
        this.name = name;
        this.os = os;
    }

    public static Platform get() {
        return ANDROID;
    }

    public static Architecture getArchitecture() {
        return switch (osArch()) {
            case "aarch64", "arm64" -> Architecture.ARM64;
            case "arm", "armv7", "armv7l", "armeabi" -> Architecture.ARM32;
            case "x86_64", "amd64" -> Architecture.X64;
            case "x86", "i386" -> Architecture.X86;
            default -> Architecture.ARM64;
        };
    }

    public static String getArchitectureString() {
        return getArchitecture().name().toLowerCase(java.util.Locale.ROOT);
    }

    private static String osArch() {
        return System.getProperty("os.arch", "aarch64").toLowerCase(java.util.Locale.ROOT);
    }

    public static String getName() {
        return get().name;
    }

    public static String getOs() {
        return get().os;
    }

    public String getName0() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public enum Architecture {
        X86("x86"),
        X64("x64"),
        ARM32("arm32"),
        ARM64("arm64"),
        RISCV64("riscv64");

        private final String name;

        Architecture(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}