package org.lwjgl.util.spvc;

/**
 * Reflected shader resource entry (mirrors {@code spvc_reflected_resource}). Pointers are
 * stub handles; {@link #nameString()} reflects the name set through {@code spvc_compiler_set_name}.
 */
public final class SpvcReflectedResource {

    private long id;
    private long typeId;
    private long baseTypeId;
    private String name = "";
    private int resourceType;

    private SpvcReflectedResource() {
    }

    public static SpvcReflectedResource create(long address) {
        SpvcReflectedResource r = new SpvcReflectedResource();
        r.id = address;
        r.typeId = address;
        r.baseTypeId = address;
        return r;
    }

    public static Buffer create(long address, int count) {
        return new Buffer(address, count);
    }

    public long id() {
        return id;
    }

    public long type_id() {
        return typeId;
    }

    public long base_type_id() {
        return baseTypeId;
    }

    public long type() {
        return typeId;
    }

    public long name() {
        return name == null || name.isEmpty() ? 0L : org.lwjgl.system.MemoryUtil.memAddress(org.lwjgl.system.MemoryUtil.memUTF8(name));
    }

    public String nameString() {
        return name;
    }

    public SpvcReflectedResource set_name(CharSequence name) {
        this.name = name == null ? "" : name.toString();
        return this;
    }

    public SpvcReflectedResource set_resource_type(int resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public SpvcReflectedResource set_type_id(long typeId) {
        this.typeId = typeId;
        return this;
    }

    public int resource_type() {
        return resourceType;
    }

    /** Lightweight array wrapper over a stub handle list. */
    public static final class Buffer {

        private final long address;
        private final int count;

        Buffer(long address, int count) {
            this.address = address;
            this.count = Math.max(0, count);
        }

        public int size() {
            return count;
        }

        public long address() {
            return address;
        }

        public SpvcReflectedResource get(int index) {
            SpvcReflectedResource r = new SpvcReflectedResource();
            r.id = address + index;
            r.typeId = address + index;
            r.baseTypeId = address + index;
            return r;
        }
    }
}