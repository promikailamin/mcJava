package org.lwjgl.openal;

/** Flags describing the emulated AL implementation. */
public final class ALCapabilities {

    public final boolean AL10 = true;
    public final boolean AL11 = true;
    public final boolean AL12 = true;
    public final boolean SOFT_positional = false;
    public final boolean SOFT_loop_points = false;
    public final boolean EXT_float32 = false;
    public final boolean EXT_mulaw = false;
    public final boolean EXT_ALAW = false;
    public final boolean SOFT_source_resampler = false;
    public final boolean EXT_BFORMAT = false;

    public ALCapabilities() {
    }
}