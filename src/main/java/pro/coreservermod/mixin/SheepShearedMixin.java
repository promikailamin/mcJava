package pro.coreservermod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.coreservermod.CoreServerMod;
import pro.coreservermod.handler.SheepWoolHandler;

@Mixin(Sheep.class)
public class SheepShearedMixin {
    @Inject(method = "setSheared", at = @At("TAIL"))
    private void onSheared(boolean sheared, CallbackInfo ci) {
        if (CoreServerMod.CONFIG.sheep_wool_regrowth_seconds <= 0) return;

        Sheep sheep = (Sheep) (Object) this;
        if (sheep.level() instanceof ServerLevel serverLevel && sheared) {
            SheepWoolHandler.scheduleRegrowth(serverLevel, sheep.getId());
        }
    }
}
