package net.minecraft.world.level.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import java.util.Optional;

public final class DimensionType {
    public static final Codec<DimensionType> CODEC = Codec.unit(new DimensionType(Optional.empty(), HolderSet.direct(), EnvironmentAttributeMap.EMPTY, 0.0f, 0.0f, 0.0f, false, false, false, 0.0f, 1.0f, false, false, 0.0f, DimensionType.MonsterSpawnType.NONE));
    
    private final Optional<Float> minY;
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
    private final boolean logicalHeight;
    private final float infiniburn;
    private final MonsterSpawnType monsterSpawnType;
    
    public DimensionType(Optional<Float> minY, HolderSet<Biome> biomes, EnvironmentAttributeMap attributes, float coordinateScale, float piglinSafe, float natural, boolean fixedTime, boolean hasSkyLight, boolean hasCeiling, float ultraWarm, float respawnAnchorWorks, float ambientLight, float infiniburn, MonsterSpawnType monsterSpawnType) {
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
        this.logicalHeight = logicalHeight;
        this.infiniburn = infiniburn;
        this.monsterSpawnType = monsterSpawnType;
    }
    
    public Optional<Float> minY() { return minY; }
    public HolderSet<Biome> biomes() { return biomes; }
    public EnvironmentAttributeMap attributes() { return attributes; }
    public float coordinateScale() { return coordinateScale; }
    public float piglinSafe() { return piglinSafe; }
    public float natural() { return natural; }
    public boolean fixedTime() { return fixedTime; }
    public boolean hasSkyLight() { return hasSkyLight; }
    public boolean hasCeiling() { return hasCeiling; }
    public float ultraWarm() { return ultraWarm; }
    public float respawnAnchorWorks() { return respawnAnchorWorks; }
    public float ambientLight() { return ambientLight; }
    public boolean logicalHeight() { return logicalHeight; }
    public float infiniburn() { return infiniburn; }
    public MonsterSpawnType monsterSpawnType() { return monsterSpawnType; }
    
    public enum MonsterSpawnType { NONE, ZOMBIE, SKELETON }
}