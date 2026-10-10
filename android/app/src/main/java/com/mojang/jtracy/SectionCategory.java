package com.mojang.jtracy;

public final class SectionCategory {

    private final String name;

    SectionCategory(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Zone enterSection(String name) {
        return Zone.NULL_ZONE;
    }

    public Zone push(String name) {
        return Zone.NULL_ZONE;
    }

    public Zone push() {
        return Zone.NULL_ZONE;
    }
}