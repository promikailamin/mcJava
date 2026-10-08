package org.lwjgl.openal;

/** ALC 1.1 additions used by the game's audio library. */
public final class ALC11 {

    private ALC11() {
    }

    public static final int ALC_MONO_SOURCES = 0x1010;
    public static final int ALC_STEREO_SOURCES = 0x1011;
    public static final int ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012;
    public static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;
    public static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;
    public static final int ALC_CAPTURE_DEVICE_SPECIFIER = 0x310;
    public static final int ALC_CAPTURE_SAMPLES = 0x312;
    public static final int ALC_CAPTURE_DEVICES = 0x320;

    public static int alcGetInteger(long device, int value) {
        return switch (value) {
            case ALC_MONO_SOURCES -> 32;
            case ALC_STEREO_SOURCES -> 32;
            default -> ALC10.alcGetInteger(device, value);
        };
    }
}