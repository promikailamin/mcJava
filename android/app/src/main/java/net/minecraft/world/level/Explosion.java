package net.minecraft.world.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface Explosion {
   static DamageSource getDefaultDamageSource(final Level level, final @Nullable Entity source) {
      return level.damageSources().explosion(source, getIndirectSourceEntity(source));
   }

static @Nullable LivingEntity getIndirectSourceEntity(final @Nullable Entity source) {
       if (source instanceof PrimedTnt) {
          PrimedTnt primedTnt = (PrimedTnt) source;
          return primedTnt.getOwner();
       } else if (source instanceof LivingEntity) {
          LivingEntity livingEntity = (LivingEntity) source;
          return livingEntity;
       } else if (source instanceof Projectile) {
          Projectile projectile = (Projectile) source;
          Entity owner = projectile.getOwner();
          if (owner instanceof LivingEntity) {
             LivingEntity livingEntity = (LivingEntity) owner;
             return livingEntity;
          }
          return null;
       } else {
          return null;
       }
    }

   ServerLevel level();

   Explosion.BlockInteraction getBlockInteraction();

   @Nullable LivingEntity getIndirectSourceEntity();

   @Nullable Entity getDirectSourceEntity();

   float radius();

   Vec3 center();

   boolean canTriggerBlocks();

   boolean shouldAffectBlocklikeEntities();

   enum BlockInteraction {
      KEEP(false),
      DESTROY(true),
      DESTROY_WITH_DECAY(true),
      TRIGGER_BLOCK(false);

      private final boolean shouldAffectBlocklikeEntities;

      BlockInteraction(final boolean shouldAffectBlocklikeEntities) {
         this.shouldAffectBlocklikeEntities = shouldAffectBlocklikeEntities;
      }

      public boolean shouldAffectBlocklikeEntities() {
         return this.shouldAffectBlocklikeEntities;
      }
   }
}
