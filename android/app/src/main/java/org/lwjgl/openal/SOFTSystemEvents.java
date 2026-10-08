package org.lwjgl.openal;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.PointerBuffer;
import java.nio.IntBuffer;

/** ALC_SOFT_system_events emulation: subscription surface only, no callbacks fired. */
public final class SOFTSystemEvents {

    private SOFTSystemEvents() {
    }

    public static final int ALC_EVENT_DEVICE_ADDED = 0x31A0;
    public static final int ALC_EVENT_DEVICE_REMOVED = 0x31A1;
    public static final int ALC_EVENT_TYPE_DEFAULT_DEVICE_CHANGED = 0x31A2;
    public static final int ALC_EVENT_DEVICE_HAS_HRTF = 0x31B0;
    public static final int ALC_EVENT_DEVICE_ERROR = 0x31B1;

    public static int alcEventIsSupportedSOFT(int eventType, int deviceType) {
        return eventType >= 6614 && eventType <= 6616 ? 6617 : 0;
    }

    public static boolean alcEventControlSOFT(IntBuffer eventTypes, boolean enable) {
        return true;
    }

    public static boolean alcEventControlSOFT(PointerBuffer eventTypes) {
        return true;
    }

    public static boolean alcEventControlSOFT(int[] eventType) {
        return true;
    }

    public static boolean alcEventCallbackSOFT(SOFTSystemEventProcI callback, long userData) {
        return true;
    }

    private static long addr(String s) {
        return MemoryUtil.nalloc(s.length() + 1);
    }
}