package pro.coreservermod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.coreservermod.CoreServerMod;

@Mixin(Animal.class)
public class AnimalEntityMixin {
    @Inject(method = "canMate", at = @At("HEAD"), cancellable = true)
    private void canMate(Animal other, CallbackInfoReturnable<Boolean> cir) {
        if (!CoreServerMod.CONFIG.animal_breeding_cooldown_disabled) return;

        Animal self = (Animal) (Object) this;
        if (self.isInLove() && other.isInLove()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void clearBreedingCooldown(ServerLevel world, Animal other, AgeableMob child, CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.animal_breeding_cooldown_disabled) return;

        Animal self = (Animal) (Object) this;
        self.setAge(0);
        other.setAge(0);
    }
}
