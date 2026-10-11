package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.TimeMarkerInfo;
import net.minecraft.world.attribute.AttributeTrack;
import net.minecraft.world.clock.WorldClock;

public final class EnvironmentAttributeSystem {
    public static final Codec<EnvironmentAttributeSystem> CODEC = Codec.unit(new EnvironmentAttributeSystem(Map.of(), Map.of(), List.of()));
    
    private final Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks;
    private final Map<ResourceKey<DimensionType>, Holder<WorldClock>> clocks;
    private final List<TimeMarkerInfo> timeMarkers;
    
    private EnvironmentAttributeSystem(Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks, Map<ResourceKey<DimensionType>, Holder<WorldClock>> clocks, List<TimeMarkerInfo> timeMarkers) {
        this.tracks = tracks;
        this.clocks = clocks;
        this.timeMarkers = timeMarkers;
    }
    
    public Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks() { return tracks; }
    public Map<ResourceKey<DimensionType>, Holder<WorldClock>> clocks() { return clocks; }
    public List<TimeMarkerInfo> timeMarkers() { return timeMarkers; }
}