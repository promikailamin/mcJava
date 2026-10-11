package net.minecraft.world.level.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.biome.Biome;
import java.util.Optional;
import java.util.function.Supplier;

public final class DimensionType {
    public static final Codec<DimensionType> CODEC = Codec.unit(new DimensionType(0, HolderSet.direct(), EnvironmentAttributeMap.EMPTY, 0.0f, 0.0f, 0.0f, false, false, false, 0.0f, 1.0f, 0.0f, 0.0f, DimensionType.MonsterSpawnType.NONE));
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DimensionType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.DIMENSION_TYPE);
    
    private final int minY;
    private final HolderSet<Biome> biomes;
    private final EnvironmentAttributeMap attributes;
    private final float coordinateScale;
    private final float piglinSafe;
    private final float natural;
    private final boolean fixedTime;
    private final boolean hasSkyLight;
    private final boolean hasCeiling;
    private final float ultraWarm;
    private final float respawnAnchorWorks;
    private final float ambientLight;
    private final int logicalHeight;
    private final float infiniburn;
    private final MonsterSpawnType monsterSpawnType;
    
    public DimensionType(int minY, HolderSet<Biome> biomes, EnvironmentAttributeMap attributes, float coordinateScale, float piglinSafe, float natural, boolean fixedTime, boolean hasSkyLight, boolean hasCeiling, float ultraWarm, float respawnAnchorWorks, float ambientLight, float infiniburn, MonsterSpawnType monsterSpawnType) {
        this.minY = minY;
        this.biomes = biomes;
        this.attributes = attributes;
        this.coordinateScale = coordinateScale;
        this.piglinSafe = piglinSafe;
        this.natural = natural;
        this.fixedTime = fixedTime;
        this.hasSkyLight = hasSkyLight;
        this.hasCeiling = hasCeiling;
        this.ultraWarm = ultraWarm;
        this.respawnAnchorWorks = respawnAnchorWorks;
        this.ambientLight = ambientLight;
        this.logicalHeight = 256;
        this.infiniburn = infiniburn;
        this.monsterSpawnType = monsterSpawnType;
    }
    
    public int minY() { return minY; }
    public HolderSet<Biome> biomes() { return biomes; }
    public EnvironmentAttributeMap attributes() { return attributes; }
    public float coordinateScale() { return coordinateScale; }
    public float piglinSafe() { return piglinSafe; }
    public float natural() { return natural; }
    public boolean fixedTime() { return fixedTime; }
    public boolean hasFixedTime() { return fixedTime; }
    public boolean hasSkyLight() { return hasSkyLight; }
    public boolean hasCeiling() { return hasCeiling; }
    public float ultraWarm() { return ultraWarm; }
    public float respawnAnchorWorks() { return respawnAnchorWorks; }
    public float ambientLight() { return ambientLight; }
    public int logicalHeight() { return logicalHeight; }
    public int height() { return 256; }
    public float infiniburn() { return infiniburn; }
    public MonsterSpawnType monsterSpawnType() { return monsterSpawnType; }
    public boolean hasEnderDragonFight() { return false; }
    public Optional<Holder<WorldClock>> defaultClock() { return Optional.empty(); }
    public static float getTeleportationScale(DimensionType from, DimensionType to) { return 1.0f; }
    public static final float[] MOON_BRIGHTNESS_PER_PHASE = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    
    public enum MonsterSpawnType { NONE, ZOMBIE, SKELETON }
}