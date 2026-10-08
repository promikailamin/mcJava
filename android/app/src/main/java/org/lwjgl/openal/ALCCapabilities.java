package org.lwjgl.openal;

/** Flags describing the emulated ALC implementation (device enumeration disabled). */
public final class ALCCapabilities {

    public final boolean ALC10 = true;
    public final boolean ALC11 = true;
    public final boolean ALC_ENUMERATION_EXT = true;
    public final boolean ALC_ENUMERATE_ALL_EXT = true;
    public final boolean ALC_EXT_disconnect = false;
    public final boolean ALC_SOFT_system_events = false;

    public ALCCapabilities() {
    }
}