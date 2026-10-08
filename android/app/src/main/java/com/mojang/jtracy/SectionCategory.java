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
        return new Zone();
    }

    public Zone push(String name) {
        return new Zone();
    }

    public Zone push() {
        return new Zone();
    }
}