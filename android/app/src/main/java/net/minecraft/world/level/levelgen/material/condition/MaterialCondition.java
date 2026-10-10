// PATTERN_SWITCHES_CONVERTED
package net.minecraft.world.level.levelgen.material.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;

public interface MaterialCondition {
   Codec<MaterialCondition> DIRECT_CODEC = BuiltInRegistries.MATERIAL_CONDITION_TYPE.byNameCodec().dispatch(MaterialCondition::codec, Function.identity());
Codec<MaterialCondition> CODEC = RegistryCodecs.holder(Registries.MATERIAL_CONDITION, DIRECT_CODEC).xmap(holder -> {
       if (holder instanceof Holder.Direct<MaterialCondition> direct) {
           return (MaterialCondition)direct.value();
       }
       else if (holder instanceof Holder.Reference<MaterialCondition> reference) {
           return new MaterialCondition.HolderHolder(reference);
       }
       else {
           throw new IllegalArgumentException("Unexpected holder type: " + holder);
       }
    }, value -> {
       if (value instanceof MaterialCondition.HolderHolder holderHolder) {
           return holderHolder.holder();
       } else {
           return Holder.direct(value);
       }
    });

   ConditionEvaluator compile(MaterialRuleContext context);

   MapCodec<? extends MaterialCondition> codec();

   record HolderHolder(Holder<MaterialCondition> holder) implements MaterialCondition {
      @Override
      public ConditionEvaluator compile(final MaterialRuleContext context) {
         return this.holder.value().compile(context);
      }

      @Override
      public MapCodec<MaterialCondition.HolderHolder> codec() {
         throw new UnsupportedOperationException("HolderHolder cannot be serialized");
      }
   }
}
