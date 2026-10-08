package com.mojang.jtracy;

public final class DiscontinuousFrame {

    private boolean active;

    DiscontinuousFrame() {
    }

    public void start() {
        active = true;
    }

    public void end() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }
}