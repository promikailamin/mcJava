// PATTERN_SWITCHES_CONVERTED
package net.minecraft.world.level.levelgen.material.rule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;

public interface MaterialRule {
   Codec<MaterialRule> DIRECT_CODEC = BuiltInRegistries.MATERIAL_RULE_TYPE.byNameCodec().dispatch(MaterialRule::codec, Function.identity());
   Codec<Holder<MaterialRule>> HOLDER_CODEC = RegistryCodecs.holder(Registries.MATERIAL_RULE, DIRECT_CODEC);
   Codec<MaterialRule> CODEC = HOLDER_CODEC.xmap(holder -> {
      if (holder instanceof Holder.Direct<MaterialRule> direct) {
          return (MaterialRule)direct.value();
      }
      else if (holder instanceof Holder.Reference<MaterialRule> reference) {
          return new MaterialRule.HolderHolder(reference);
      }
      else {
          throw new IllegalArgumentException("Unexpected holder type: " + holder)
      }
   }, value -> {
      return switch (value) {
         case MaterialRule.HolderHolder(Holder<MaterialRule> holder) -> holder;
         default -> Holder.direct(value);
      };
   });

   RuleEvaluator compile(MaterialRuleContext context);

   MapCodec<? extends MaterialRule> codec();

   record HolderHolder(Holder<MaterialRule> holder) implements MaterialRule {
      @Override
      public RuleEvaluator compile(final MaterialRuleContext context) {
         return this.holder.value().compile(context);
      }

      @Override
      public MapCodec<MaterialRule.HolderHolder> codec() {
         throw new UnsupportedOperationException("HolderHolder cannot be serialized");
      }
   }
}
