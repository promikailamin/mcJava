package net.minecraft.world.timeline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.AttributeTrack;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.level.dimension.DimensionType;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public final class Timeline {
    public static final Codec<Timeline> CODEC = Codec.unit(new Timeline(Holder.direct(WorldClock.DEFAULT), Optional.empty(), Map.of(), Map.of()));
    
    private final Holder<WorldClock> clock;
    private final Optional<Integer> periodTicks;
    private final Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks;
    private final Map<ResourceKey<DimensionType>, TimeMarkerInfo> timeMarkers;
    
    public Timeline(Holder<WorldClock> clock, Optional<Integer> periodTicks, Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks, Map<ResourceKey<DimensionType>, TimeMarkerInfo> timeMarkers) {
        this.clock = clock;
        this.periodTicks = periodTicks;
        this.tracks = tracks;
        this.timeMarkers = timeMarkers;
    }
    
    public Holder<WorldClock> clock() { return clock; }
    public Optional<Integer> periodTicks() { return periodTicks; }
    public Map<EnvironmentAttribute<?>, AttributeTrack<?, ?>> tracks() { return tracks; }
    public Map<ResourceKey<DimensionType>, TimeMarkerInfo> timeMarkers() { return timeMarkers; }
}