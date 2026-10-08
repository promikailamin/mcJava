package org.lwjgl;

/** LWJGL version surface. */
public final class Version {

    private static final String VERSION = "3.4.3";

    private Version() {
    }

    public static String getVersion() {
        return VERSION;
    }
}