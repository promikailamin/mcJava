package net.minecraft.world.timeline;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;

public record TimeMarkerInfo(ResourceKey<DimensionType> dimension, long time, long tick) {
    public static final Codec<TimeMarkerInfo> CODEC = Codec.unit(new TimeMarkerInfo(ResourceKey.create(Registry.DIMENSION_TYPE_REGISTRY, net.minecraft.resources.Identifier.of("overworld")), 0L, 0L));
}