package pro.codeservermod.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.codeservermod.CoreServerMod;
import pro.codeservermod.handler.IronGolemHandler;

@Mixin(VillagerEntity.class)
public class VillagerWakeMixin {
    @Inject(method = "onWakingUp", at = @At("TAIL"))
    private void onWakingUp(CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.iron_golem_spawn_enabled) return;
        
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (villager.getWorld() instanceof ServerWorld serverWorld) {
            IronGolemHandler.scheduleGolemCheck(serverWorld, villager.getBlockPos());
        }
    }
}