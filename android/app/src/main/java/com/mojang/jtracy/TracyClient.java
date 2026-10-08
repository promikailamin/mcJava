package com.mojang.jtracy;

/**
 * In-memory Tracy client surface. Real Tracy protocol capture is not wired up in this port;
 * all entry points are accepted and ignored so upstream profiling calls compile and no-op.
 */
public final class TracyClient {

    private TracyClient() {
    }

    public static void load() {
    }

    public static boolean isAvailable() {
        return false;
    }

    public static void reportAppInfo(String appInfo) {
    }

    public static void message(String text, int color) {
    }

    public static void message(String text, long color) {
    }

    public static void setThreadName(String name, int id) {
    }

    public static SectionCategory createSectionCategory(String name) {
        return new SectionCategory(name);
    }

    public static Plot createPlot(String name) {
        return new Plot(name);
    }

    public static MemoryPool createMemoryPool(String name) {
        return new MemoryPool(name);
    }

    public static DiscontinuousFrame createDiscontinuousFrame(String name) {
        return new DiscontinuousFrame();
    }

    public static Zone beginZone(String name, boolean onlyIfEnabled) {
        return new Zone();
    }

    public static Zone beginZone(String name, String function, String file, int line) {
        return new Zone();
    }

    public static GpuContext createGpuContext(GpuApi api, long timestampPeriod, float contextPeriod) {
        return new GpuContext();
    }

    public static void frameImage(java.nio.ByteBuffer image, int width, int height, double delay, boolean flipVertically) {
    }

    public static void frameImage(long image, int width, int height, double delay, boolean flipVertically) {
    }

    public static void markFrame() {
    }
}