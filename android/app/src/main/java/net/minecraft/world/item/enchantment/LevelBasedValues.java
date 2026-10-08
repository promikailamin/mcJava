package net.minecraft.world.item.enchantment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.Mth;

public final class LevelBasedValues {
   private LevelBasedValues() {}

   public record Constant(float value) implements LevelBasedValue {
      public static final Codec<Constant> CODEC = Codec.FLOAT.xmap(Constant::new, Constant::value);
      public static final MapCodec<Constant> TYPED_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Codec.FLOAT.fieldOf("value").forGetter(Constant::value)).apply(i, Constant::new)
      );

      @Override
      public float calculate(final int level) {
         return this.value;
      }

      @Override
      public MapCodec<Constant> codec() {
         return TYPED_CODEC;
      }
   }

   public record Clamped(LevelBasedValue value, float min, float max) implements LevelBasedValue {
      public static final MapCodec<Clamped> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  LevelBasedValue.CODEC.fieldOf("value").forGetter(c -> c.value()),
                  Codec.FLOAT.fieldOf("min").forGetter(c -> c.min()),
                  Codec.FLOAT.fieldOf("max").forGetter(c -> c.max())
               )
               .apply(i, Clamped::new)
         )
         .validate(Clamped::validateRange);

      private static DataResult<Clamped> validateRange(final Clamped u) {
         return u.max <= u.min ? DataResult.error(() -> "Max must be larger than min, min: " + u.min + ", max: " + u.max) : DataResult.success(u);
      }

      @Override
      public float calculate(final int level) {
         return Mth.clamp(this.value.calculate(level), this.min, this.max);
      }

      @Override
      public MapCodec<Clamped> codec() {
         return CODEC;
      }
   }

   public record Exponent(LevelBasedValue base, LevelBasedValue power) implements LevelBasedValue {
      public static final MapCodec<Exponent> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               LevelBasedValue.CODEC.fieldOf("base").forGetter(e -> e.base()),
               LevelBasedValue.CODEC.fieldOf("power").forGetter(e -> e.power())
            )
            .apply(i, Exponent::new)
      );

      @Override
      public float calculate(final int level) {
         return (float)Math.pow(this.base.calculate(level), this.power.calculate(level));
      }

      @Override
      public MapCodec<Exponent> codec() {
         return CODEC;
      }
   }

   public record Fraction(LevelBasedValue numerator, LevelBasedValue denominator) implements LevelBasedValue {
      public static final MapCodec<Fraction> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               LevelBasedValue.CODEC.fieldOf("numerator").forGetter(f -> f.numerator()),
               LevelBasedValue.CODEC.fieldOf("denominator").forGetter(f -> f.denominator())
            )
            .apply(i, Fraction::new)
      );

      @Override
      public float calculate(final int level) {
         float denominator = this.denominator.calculate(level);
         return denominator == 0.0F ? 0.0F : this.numerator.calculate(level) / denominator;
      }

      @Override
      public MapCodec<Fraction> codec() {
         return CODEC;
      }
   }

   public record LevelsSquared(float added) implements LevelBasedValue {
      public static final MapCodec<LevelsSquared> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(Codec.FLOAT.fieldOf("added").forGetter(ls -> ls.added())).apply(i, LevelsSquared::new)
      );

      @Override
      public float calculate(final int level) {
         return Mth.square(level) + this.added;
      }

      @Override
      public MapCodec<LevelsSquared> codec() {
         return CODEC;
      }
   }

   public record Linear(float base, float perLevelAboveFirst) implements LevelBasedValue {
      public static final MapCodec<Linear> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.FLOAT.fieldOf("base").forGetter(l -> l.base()),
               Codec.FLOAT.fieldOf("per_level_above_first").forGetter(l -> l.perLevelAboveFirst())
            )
            .apply(i, Linear::new)
      );

      @Override
      public float calculate(final int level) {
         return this.base + this.perLevelAboveFirst * (level - 1);
      }

      @Override
      public MapCodec<Linear> codec() {
         return CODEC;
      }
   }

   public record Lookup(List<Float> values, LevelBasedValue fallback) implements LevelBasedValue {
      public static final MapCodec<Lookup> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.FLOAT.listOf().fieldOf("values").forGetter(l -> l.values()),
               LevelBasedValue.CODEC.fieldOf("fallback").forGetter(l -> l.fallback())
            )
            .apply(i, Lookup::new)
      );

      @Override
      public float calculate(final int level) {
         return level <= this.values.size() ? this.values.get(level - 1) : this.fallback.calculate(level);
      }

      @Override
      public MapCodec<Lookup> codec() {
         return CODEC;
      }
   }
}
