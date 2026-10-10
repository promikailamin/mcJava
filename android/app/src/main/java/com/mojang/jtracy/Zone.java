package com.mojang.jtracy;

public final class Zone implements AutoCloseable {
    public static final Zone NULL_ZONE = new Zone();

    private Zone() {}

    @Override
    public void close() {
    }

    public void addText(String text) {
    }

    public void addValue(long value) {
    }

    public void setColor(int color) {
    }
}