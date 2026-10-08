package net.minecraft.world.item.enchantment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;

public interface LevelBasedValue {
   Codec<LevelBasedValue> DISPATCH_CODEC = BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.byNameCodec().dispatch(LevelBasedValue::codec, c -> c);

   static final class CodecHolder {
      static final Codec<LevelBasedValue> CODEC = Codec.either(LevelBasedValues.Constant.CODEC, DISPATCH_CODEC)
         .xmap(
            either -> (LevelBasedValue)either.map(l -> l, r -> r),
            levelBasedValue -> levelBasedValue instanceof LevelBasedValues.Constant constant ? Either.left(constant) : Either.right(levelBasedValue)
         );
   }

   static MapCodec<? extends LevelBasedValue> bootstrap(final Registry<MapCodec<? extends LevelBasedValue>> registry) {
      Registry.register(registry, "clamped", LevelBasedValues.Clamped.CODEC);
      Registry.register(registry, "fraction", LevelBasedValues.Fraction.CODEC);
      Registry.register(registry, "levels_squared", LevelBasedValues.LevelsSquared.CODEC);
      Registry.register(registry, "linear", LevelBasedValues.Linear.CODEC);
      Registry.register(registry, "exponent", LevelBasedValues.Exponent.CODEC);
      return Registry.register(registry, "lookup", LevelBasedValues.Lookup.CODEC);
   }

   static LevelBasedValue constant(final float value) {
      return new LevelBasedValues.Constant(value);
   }

   static LevelBasedValue.Linear perLevel(final float base, final float perLevelAboveFirst) {
      return new LevelBasedValues.Linear(base, perLevelAboveFirst);
   }

   static LevelBasedValue.Linear perLevel(final float perLevel) {
      return perLevel(perLevel, perLevel);
   }

   static LevelBasedValue.Lookup lookup(final List<Float> values, final LevelBasedValue fallback) {
      return new LevelBasedValues.Lookup(values, fallback);
   }

   float calculate(int level);

   MapCodec<? extends LevelBasedValue> codec();
}
