package org.lwjgl.openal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** ALC 1.0 device/context surface. Devices and contexts are fake handles. */
public final class ALC10 {

    private static final java.util.concurrent.atomic.AtomicLong NEXT = new java.util.concurrent.atomic.AtomicLong(0x6000L);

    public static final int ALC_INVALID = 0xFFFFFFFF;
    public static final int ALC_FALSE = 0x0;
    public static final int ALC_TRUE = 0x1;
    public static final int ALC_NO_ERROR = 0x0;
    public static final int ALC_MAJOR_VERSION = 0x1000;
    public static final int ALC_MINOR_VERSION = 0x1001;
    public static final int ALC_ATTRIBUTES_SIZE = 0x1002;
    public static final int ALC_ALL_ATTRIBUTES = 0x1003;
    public static final int ALC_FREQUENCY = 0x1007;
    public static final int ALC_REFRESH = 0x1008;
    public static final int ALC_SYNC = 0x1009;
    public static final int ALC_DEFAULT_DEVICE_SPECIFIER = 0x1004;
    public static final int ALC_DEVICE_SPECIFIER = 0x1005;
    public static final int ALC_EXTENSIONS = 0x1006;
    public static final int ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012;
    public static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;
    public static final int ALC_CAPTURE_DEVICE = 0x310;
    public static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;

    private ALC10() {
    }

    public static long alcOpenDevice(ByteBuffer deviceSpecifier) {
        return NEXT.incrementAndGet();
    }

    public static long alcOpenDevice(CharSequence deviceSpecifier) {
        return ALC.getDefaultDevice();
    }

    public static boolean alcCloseDevice(long device) {
        return true;
    }

    public static long alcCreateContext(long device, IntBuffer attrList) {
        return NEXT.incrementAndGet();
    }

    public static boolean alcMakeContextCurrent(long context) {
        return true;
    }

    public static boolean alcDestroyContext(long context) {
        return true;
    }

    public static long alcGetCurrentContext() {
        return 0L;
    }

    public static long alcGetContextsDevice(long context) {
        return ALC.getDefaultDevice();
    }

    public static int alcGetError(long device) {
        return 0;
    }

    public static boolean alcIsExtensionPresent(long device, ByteBuffer extName) {
        return false;
    }

    public static boolean alcIsExtensionPresent(long device, CharSequence extName) {
        return "ALC_ENUMERATION_EXT".contentEquals(extName);
    }

    public static int alcGetInteger(long device, int param) {
        return switch (param) {
            case ALC_FREQUENCY -> mixer_state.MASTER_RATE;
            case ALC_REFRESH -> 60;
            case ALC_MAJOR_VERSION -> 1;
            case ALC_MINOR_VERSION -> 1;
            default -> 0;
        };
    }

    public static void alcGetIntegerv(long device, int param, IntBuffer values) {
        values.put(0, alcGetInteger(device, param));
    }

    public static ByteBuffer alcGetString(long device, int param) {
        String s = switch (param) {
            case ALC_DEFAULT_ALL_DEVICES_SPECIFIER, ALC_ALL_DEVICES_SPECIFIER,
                 ALC_DEFAULT_DEVICE_SPECIFIER, ALC_DEVICE_SPECIFIER -> "opencode-soft";
            default -> "";
        };
        return java.nio.charset.StandardCharsets.UTF_8.encode(s);
    }

    public static String alcGetStringString(long device, int param) {
        ByteBuffer buffer = alcGetString(device, param);
        if (buffer == null) {
            return null;
        }
        ByteBuffer b = buffer.duplicate();
        StringBuilder sb = new StringBuilder(b.remaining());
        while (b.hasRemaining()) {
            byte c = b.get();
            if (c == 0) {
                break;
            }
            sb.append((char) (c & 0xFF));
        }
        return sb.toString();
    }
}