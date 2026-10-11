package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import java.util.Optional;

public final class Biome {
    public static final Codec<Biome> CODEC = Codec.unit(new Biome(EnvironmentAttributeMap.EMPTY, Optional.empty(), Optional.empty()));
    
    private final EnvironmentAttributeMap attributes;
    private final Optional<ResourceKey<DimensionType>> dimension;
    private final Optional<Float> temperature;
    
    private Biome(EnvironmentAttributeMap attributes, Optional<ResourceKey<DimensionType>> dimension, Optional<Float> temperature) {
        this.attributes = attributes;
        this.dimension = dimension;
        this.temperature = temperature;
    }
    
    public EnvironmentAttributeMap getAttributes() { return attributes; }
    public Optional<ResourceKey<DimensionType>> dimension() { return dimension; }
    public Optional<Float> temperature() { return temperature; }
    
    public static class ClimateSettings {
        public final float temperature;
        public final float downfall;
        public final float snow;
        public final float rain;
        
        public ClimateSettings(float temperature, float downfall, float snow, float rain) {
            this.temperature = temperature;
            this.downfall = downfall;
            this.snow = snow;
            this.rain = rain;
        }
    }
}