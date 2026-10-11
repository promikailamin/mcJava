package net.minecraft.world.attribute;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.Holder;

public final class SpatialAttributeInterpolator {
    private final Map<EnvironmentAttributeMap, Double> weightsBySource = Maps.newHashMap();
    
    public void addSource(EnvironmentAttributeMap source, double weight) {
        weightsBySource.put(source, weight);
    }
}