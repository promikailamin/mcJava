package org.lwjgl.openal;

@FunctionalInterface
public interface SOFTSystemEventProcI {

    /** Mirrors {@code SOFTSystemEventProc}: event type, device type, device, message length, message ptr, user data. */
    void invoke(int eventType, int deviceType, long device, long messageLength, long messagePtr, long userData);

    static SOFTSystemEventProcI create(SOFTSystemEventProcI callback) {
        return callback;
    }
}