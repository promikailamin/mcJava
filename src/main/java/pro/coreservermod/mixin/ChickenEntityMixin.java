package pro.coreservermod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.coreservermod.CoreServerMod;
import pro.coreservermod.handler.ChickenEggHandler;

@Mixin(Entity.class)
public class ChickenEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onEntityTick(CallbackInfo ci) {
        if (CoreServerMod.CONFIG.chicken_egg_interval_seconds <= 0) return;

        Entity entity = (Entity) (Object) this;
        if (entity instanceof Chicken chicken
                && entity.level() instanceof ServerLevel serverLevel
                && !chicken.isBaby()) {
            ChickenEggHandler.scheduleEggLay(serverLevel, chicken.getId());
        }
    }
}
