package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import java.util.Optional;
import java.util.function.Supplier;

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
    
    public enum Precipitation {
        RAIN,
        SNOW,
        NONE
    }
    
    public static class BiomeBuilder {
        private EnvironmentAttributeMap attributes = EnvironmentAttributeMap.EMPTY;
        private Optional<ResourceKey<DimensionType>> dimension = Optional.empty();
        private Optional<Float> temperature = Optional.empty();
        private Precipitation precipitation = Precipitation.NONE;
        private float downfall = 0.0f;
        private float temperature_ = 0.0f;
        
        public BiomeBuilder precipitation(Precipitation precipitation) {
            this.precipitation = precipitation;
            return this;
        }
        
        public BiomeBuilder downfall(float downfall) {
            this.downfall = downfall;
            return this;
        }
        
        public BiomeBuilder temperature(float temperature) {
            this.temperature_ = temperature;
            return this;
        }
        
        public Biome build() {
            return new Biome(attributes, dimension, Optional.of(temperature_));
        }
    }
    
    public Precipitation getPrecipitationAt(net.minecraft.core.BlockPos pos, int seaLevel) {
        return Precipitation.NONE;
    }
    
    public boolean shouldFreeze(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return false;
    }
    
    public boolean shouldSnow(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return false;
    }
}