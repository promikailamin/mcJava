package com.mojang.jtracy;

public final class TracyClient {
    private TracyClient() {}

    public static boolean isAvailable() {
        return false;
    }

    public static Zone beginZone(String name, boolean runningInIde) {
        return Zone.NULL_ZONE;
    }

    public static Zone beginZone(String name, String function, String file, int line) {
        return Zone.NULL_ZONE;
    }

    public static DiscontinuousFrame createDiscontinuousFrame(String name) {
        return new DiscontinuousFrame();
    }

    public static Plot createPlot(String name) {
        return new Plot();
    }

    public static void setThreadName(String name, int hashCode) {
    }
}