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
public class SheepEntityMixin {
    @Inject(method = "eat", at = @At("TAIL"))
    private void eat(CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.sheep_wool_regrowth_seconds > 0) return;
        
        SheepEntity sheep = (SheepEntity) (Object) this;
        if (sheep.getWorld() instanceof ServerWorld serverWorld) {
            SheepWoolHandler.scheduleRegrowth(serverWorld, sheep.getId());
        }
    }

    @Inject(method = "onSheared", at = @At("TAIL"))
    private void onSheared(CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.sheep_wool_regrowth_seconds > 0) return;
        
        SheepEntity sheep = (SheepEntity) (Object) this;
        if (sheep.getWorld() instanceof ServerWorld serverWorld) {
            SheepWoolHandler.scheduleRegrowth(serverWorld, sheep.getId());
        }
    }
}