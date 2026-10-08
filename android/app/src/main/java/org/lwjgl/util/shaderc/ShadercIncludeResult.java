package org.lwjgl.util.shaderc;

import org.lwjgl.system.MemoryUtil;

/**
 * {@code shaderc_include_result} holder. Layout mirrors the C struct (name/content pointers),
 * backed by {@link MemoryUtil} so only the two address fields are significant.
 */
public final class ShadercIncludeResult {

    public static final int SOURCE_NAME = 0;
    public static final int CONTENT = 8;

    private final long address;
    private long sourceName;
    private long content;

    private ShadercIncludeResult() {
        this.address = MemoryUtil.ncalloc(128, 8);
    }

    public static ShadercIncludeResult calloc() {
        return new ShadercIncludeResult();
    }

    public long address() {
        return address;
    }

    public static ShadercIncludeResult create(long address) {
        ShadercIncludeResult r = new ShadercIncludeResult();
        r.sourceName = MemoryUtil.memGetAddress(address + SOURCE_NAME);
        r.content = MemoryUtil.memGetAddress(address + CONTENT);
        return r;
    }

    public ShadercIncludeResult source_name(long sourceName) {
        this.sourceName = sourceName;
        MemoryUtil.memPutAddress(address + SOURCE_NAME, sourceName);
        return this;
    }

    public ShadercIncludeResult content(long content) {
        this.content = content;
        MemoryUtil.memPutAddress(address + CONTENT, content);
        return this;
    }

    public long source_name() {
        return sourceName;
    }

    public long content() {
        return content;
    }

    public void free() {
        MemoryUtil.nfree(address);
    }
}