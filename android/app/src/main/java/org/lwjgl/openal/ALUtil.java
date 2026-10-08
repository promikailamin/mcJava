package org.lwjgl.openal;

/** String-array helpers for ALC device enumeration. */
public final class ALUtil {

    private ALUtil() {
    }

    public static String[] getStringList(long device, int token) {
        String value = ALC10.alcGetString(device, token);
        if (value == null || value.isEmpty()) {
            return new String[0];
        }
        return value.split("\0");
    }
}