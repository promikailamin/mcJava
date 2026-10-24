package pro.codeservermod.mixin;

import net.minecraft.entity.AnimalEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.codeservermod.CoreServerMod;

@Mixin(AnimalEntity.class)
public class AnimalEntityMixin {
    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void canBreed(AnimalEntity other, CallbackInfoReturnable<Boolean> cir) {
        if (!CoreServerMod.CONFIG.animal_breeding_cooldown_disabled) return;
        
        AnimalEntity self = (AnimalEntity) (Object) this;
        if (self.isInLove() && other.isInLove()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setBreedingAge", at = @At("HEAD"), cancellable = true)
    private void setBreedingAge(int age, CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.animal_breeding_cooldown_disabled) return;
        
        ci.cancel();
    }
}