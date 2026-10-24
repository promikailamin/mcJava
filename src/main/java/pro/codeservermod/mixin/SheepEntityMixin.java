package pro.codeservermod.mixin;

import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.codeservermod.CoreServerMod;
import pro.codeservermod.handler.SheepWoolHandler;

@Mixin(AnimalEntity.class)
public class SheepEntityMixin {
    @Inject(method = "eat", at = @At("TAIL"))
    private void eat(CallbackInfo ci) {
        if (CoreServerMod.CONFIG.sheep_wool_regrowth_seconds <= 0) return;
        
        AnimalEntity animal = (AnimalEntity) (Object) this;
        if (animal instanceof net.minecraft.entity.passive.SheepEntity sheep && animal.getWorld() instanceof ServerWorld serverWorld) {
            SheepWoolHandler.scheduleRegrowth(serverWorld, sheep.getId());
        }
    }
}