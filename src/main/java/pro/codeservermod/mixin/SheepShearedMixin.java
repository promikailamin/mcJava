package pro.codeservermod.mixin;

import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.codeservermod.CoreServerMod;
import pro.codeservermod.handler.SheepWoolHandler;

@Mixin(SheepEntity.class)
public class SheepShearedMixin {
    @Inject(method = "setSheared", at = @At("TAIL"))
    private void onSheared(boolean sheared, CallbackInfo ci) {
        if (CoreServerMod.CONFIG.sheep_wool_regrowth_seconds <= 0) return;
        
        SheepEntity sheep = (SheepEntity) (Object) this;
        if (sheep.getWorld() instanceof ServerWorld serverWorld && sheared) {
            SheepWoolHandler.scheduleRegrowth(serverWorld, sheep.getId());
        }
    }
}