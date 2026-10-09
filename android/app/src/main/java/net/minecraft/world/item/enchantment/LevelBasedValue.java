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

   record Constant(float value) implements LevelBasedValue {
      public static final Codec<LevelBasedValue.Constant> CODEC = Codec.FLOAT.xmap(LevelBasedValue.Constant::new, LevelBasedValue.Constant::value);
      public static final MapCodec<LevelBasedValue.Constant> TYPED_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Codec.FLOAT.fieldOf("value").forGetter(LevelBasedValue.Constant::value)).apply(i, LevelBasedValue.Constant::new)
      );

      @Override
      public float calculate(final int level) {
         return this.value;
      }

      @Override
      public MapCodec<LevelBasedValue.Constant> codec() {
         return TYPED_CODEC;
      }
   }

   Codec<LevelBasedValue> CODEC = Codec.either(LevelBasedValue.Constant.CODEC, DISPATCH_CODEC)
      .xmap(
         either -> (LevelBasedValue)either.map(l -> l, r -> r),
         levelBasedValue -> levelBasedValue instanceof LevelBasedValue.Constant constant ? Either.left(constant) : Either.right(levelBasedValue)
      );
      }
   }

   Codec<LevelBasedValue> CODEC = Codec.either(LevelBasedValue.Constant.CODEC, DISPATCH_CODEC)
      .xmap(
         either -> (LevelBasedValue)either.map(l -> l, r -> r),
         levelBasedValue -> levelBasedValue instanceof LevelBasedValue.Constant constant ? Either.left(constant) : Either.right(levelBasedValue)
      );

   static MapCodec<? extends LevelBasedValue> bootstrap(final Registry<MapCodec<? extends LevelBasedValue>> registry) {
      Registry.register(registry, "clamped", LevelBasedValue.Clamped.CODEC);
      Registry.register(registry, "fraction", LevelBasedValue.Fraction.CODEC);
      Registry.register(registry, "levels_squared", LevelBasedValue.LevelsSquared.CODEC);
      Registry.register(registry, "linear", LevelBasedValue.Linear.CODEC);
      Registry.register(registry, "exponent", LevelBasedValue.Exponent.CODEC);
      return Registry.register(registry, "lookup", LevelBasedValue.Lookup.CODEC);
   }
