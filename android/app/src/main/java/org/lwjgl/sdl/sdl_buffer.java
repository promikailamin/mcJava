package org.lwjgl.sdl;

import java.util.ArrayList;
import java.util.List;

/** Minimal struct-buffer shared by SDL structs (mirrors LWJGL {@code StructBuffer} surface). */
public abstract class sdl_buffer<T> {

    protected final List<T> elements = new ArrayList<>();
    protected int position;

    protected sdl_buffer(int capacity) {
        for (int i = 0; i < capacity; i++) {
            elements.add(newElement());
        }
    }

    protected abstract T newElement();

    public int capacity() {
        return elements.size();
    }

    public int limit() {
        return elements.size();
    }

    public int position() {
        return position;
    }

    public sdl_buffer<T> position(int pos) {
        this.position = pos;
        return this;
    }

    public T get() {
        return get(position++);
    }

    public T get(int index) {
        return elements.get(index);
    }

    public T first() {
        return elements.get(0);
    }

    public int remaining() {
        return limit() - position;
    }

    public T current() {
        return elements.get(position);
    }
}