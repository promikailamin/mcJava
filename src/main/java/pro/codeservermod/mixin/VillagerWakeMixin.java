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
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.iron_golem_spawn_enabled) return;
        
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (villager.getWorld() instanceof ServerWorld serverWorld) {
            // Check if villager is standing on iron block
            if (serverWorld.getBlockState(villager.getBlockPos().down()).isOf(net.minecraft.block.Blocks.IRON_BLOCK)) {
                IronGolemHandler.scheduleGolemCheck(serverWorld, villager.getBlockPos());
            }
        }
    }
}