package pro.codeservermod.mixin;

import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.codeservermod.CoreServerMod;
import pro.codeservermod.handler.ChickenEggHandler;

@Mixin(ChickenEntity.class)
public class ChickenEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.chicken_egg_interval_seconds > 0) return;
        
        ChickenEntity chicken = (ChickenEntity) (Object) this;
        if (chicken.getWorld() instanceof ServerWorld serverWorld && !chicken.isBaby()) {
            ChickenEggHandler.scheduleEggLay(serverWorld, chicken.getId());
        }
    }
}