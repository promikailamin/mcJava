package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class EnvironmentAttributeMap {
    public static final EnvironmentAttributeMap EMPTY = new EnvironmentAttributeMap(Map.of());
    
    private final Map<EnvironmentAttribute<?>, Object> entries;
    
    private EnvironmentAttributeMap(final Map<EnvironmentAttribute<?>, Object> entries) {
        this.entries = entries;
    }
    
    public static EnvironmentAttributeMap.Builder builder() {
        return new EnvironmentAttributeMap.Builder();
    }
    
    public boolean contains(final EnvironmentAttribute<?> attribute) {
        return this.entries.containsKey(attribute);
    }
    
    public Set<EnvironmentAttribute<?>> keySet() {
        return this.entries.keySet();
    }
    
    @Override
    public boolean equals(final Object obj) {
       if (obj == this) {
          return true;
       } else if (obj instanceof EnvironmentAttributeMap other) {
          return this.entries.equals(other.entries);
       }
       return false;
    }
    
    @Override
    public int hashCode() {
        return this.entries.hashCode();
    }
    
    @Override
    public String toString() {
        return this.entries.toString();
    }
    
    public static final Codec<EnvironmentAttributeMap> CODEC = Codec.unit(EMPTY);
    public static final Codec<EnvironmentAttributeMap> NETWORK_CODEC = CODEC;
    public static final Codec<EnvironmentAttributeMap> CODEC_ONLY_POSITIONAL = CODEC;
    
    public <Value> Entry<Value, ?> get(final EnvironmentAttribute<Value> attribute) {
        @SuppressWarnings("unchecked")
        Entry<Value, ?> entry = (Entry<Value, ?>) this.entries.get(attribute);
        return entry;
    }
    
    public static class Builder {
        public EnvironmentAttributeMap.Builder putAll(final EnvironmentAttributeMap map) {
            return this;
        }
        
        public <Value, Parameter> EnvironmentAttributeMap.Builder modify(
                final EnvironmentAttribute<Value> attribute, final net.minecraft.world.attribute.modifier.AttributeModifier<Value, Parameter> modifier, final Parameter value) {
            return this;
        }
        
        public <Value> EnvironmentAttributeMap.Builder set(final EnvironmentAttribute<Value> attribute, final Value value) {
            return this;
        }
        
        public EnvironmentAttributeMap build() {
            return EMPTY;
        }
    }
    
    public record Entry<Value, Argument>(Argument argument, net.minecraft.world.attribute.modifier.AttributeModifier<Value, Argument> modifier) {
        public Value applyModifier(Value base) {
            return modifier.apply(base, argument);
        }
    }
}