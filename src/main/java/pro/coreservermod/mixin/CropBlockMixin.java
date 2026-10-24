package pro.coreservermod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.coreservermod.CoreServerMod;

@Mixin(CropBlock.class)
public class CropBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!(CoreServerMod.CONFIG.crop_growth_time_seconds > 0)) return;

        CropBlock crop = (CropBlock) (Object) this;
        int maxAge = crop.getMaxAge();
        int currentAge = crop.getAge(state);

        if (currentAge < maxAge) {
            if (random.nextInt(20 * CoreServerMod.CONFIG.crop_growth_time_seconds) == 0) {
                world.setBlockAndUpdate(pos, crop.getStateForAge(currentAge + 1));
            }
            ci.cancel();
        }
    }
}
