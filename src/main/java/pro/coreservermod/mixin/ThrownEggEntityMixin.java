package pro.coreservermod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.coreservermod.CoreServerMod;
import pro.coreservermod.handler.ChickenSpawnHandler;

@Mixin(ThrownEgg.class)
public class ThrownEggEntityMixin {
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void onHit(HitResult hitResult, CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.guaranteed_chicken_from_egg) return;

        ThrownEgg egg = (ThrownEgg) (Object) this;
        if (egg.level() instanceof ServerLevel serverLevel) {
            ChickenSpawnHandler.spawnChickenFromEgg(serverLevel, egg.blockPosition(), egg.position());
            egg.discard();
            ci.cancel();
        }
    }
}
