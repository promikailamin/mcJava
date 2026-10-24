package pro.codeservermod.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.codeservermod.CoreServerMod;
import pro.codeservermod.handler.ChickenSpawnHandler;

@Mixin(EggEntity.class)
public class ThrownEggEntityMixin {
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onCollision(HitResult hitResult, CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.guaranteed_chicken_from_egg) return;
        
        EggEntity egg = (EggEntity) (Object) this;
        if (egg.getWorld() instanceof ServerWorld serverWorld) {
            ChickenSpawnHandler.spawnChickenFromEgg(serverWorld, egg.getBlockPos(), egg.getPos());
            ci.cancel();
        }
    }
}