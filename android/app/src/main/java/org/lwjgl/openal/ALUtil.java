package org.lwjgl.openal;

/** String-array helpers for ALC device enumeration. */
public final class ALUtil {

    private ALUtil() {
    }

    public static String[] getStringList(long device, int token) {
        java.nio.ByteBuffer buffer = ALC10.alcGetString(device, token);
        if (buffer == null || !buffer.hasRemaining()) {
            return new String[0];
        }
        java.nio.ByteBuffer b = buffer.duplicate();
        StringBuilder sb = new StringBuilder(b.remaining() + 1);
        while (b.hasRemaining()) {
            byte c = b.get();
            if (c == 0) {
                sb.append('\0');
            } else {
                sb.append((char) (c & 0xFF));
            }
        }
        String value = sb.toString().trim();
        if (value.isEmpty()) {
            return new String[0];
        }
        return value.split("\0");
    }
}