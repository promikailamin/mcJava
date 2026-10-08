package org.lwjgl.util.shaderc;

/** Include-result release callback, invoked on the game thread after shaderc consumes an include. */
public interface ShadercIncludeResultRelease {

    void invoke(long userdata, long include_result);

    default void close() {
    }

    static ShadercIncludeResultRelease create(ShadercIncludeResultRelease callback) {
        return callback;
    }
}