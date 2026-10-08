package org.lwjgl.sdl;

/**
 * Shared base for SDL "native structs". Field data lives in a {@code Map} owned by the
 * root {@link SDL_Event}, so typed event views can share one backing store. Mirrors the
 * LWJGL struct lifecycle surface the game uses ({@code malloc}/{@code calloc}/{@code close}).
 */
public abstract class sdl_struct {

    protected final java.util.Map<String, Object> fields;
    protected final String prefix;

    protected sdl_struct(java.util.Map<String, Object> fields, String prefix) {
        this.fields = fields;
        this.prefix = prefix;
    }

    protected sdl_struct() {
        this(new java.util.concurrent.ConcurrentHashMap<>(), "");
    }

    public abstract long sizeof();

    public long address() {
        return 0L;
    }

    public sdl_struct retain() {
        return this;
    }

    public sdl_struct release() {
        return this;
    }

    public void close() {
    }

    protected int int_(String name) {
        Object v = fields.get(prefix + name);
        return v instanceof Number n ? n.intValue() : 0;
    }

    protected double double_(String name) {
        Object v = fields.get(prefix + name);
        return v instanceof Number n ? n.doubleValue() : 0d;
    }

    protected float float_(String name) {
        Object v = fields.get(prefix + name);
        return v instanceof Number n ? n.floatValue() : 0f;
    }

    protected String string_(String name) {
        Object v = fields.get(prefix + name);
        return v instanceof String s ? s : null;
    }

    protected byte[] bytes_(String name) {
        Object v = fields.get(prefix + name);
        return v instanceof byte[] b ? b : null;
    }

    protected sdl_struct put_(String name, Object value) {
        fields.put(prefix + name, value);
        return this;
    }

    protected static java.util.Map<String, Object> newMap() {
        return new java.util.concurrent.ConcurrentHashMap<>();
    }
}