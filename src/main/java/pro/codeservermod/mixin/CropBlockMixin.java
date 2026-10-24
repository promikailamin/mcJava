package pro.codeservermod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.codeservermod.CoreServerMod;

@Mixin(CropBlock.class)
public class CropBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!(CoreServerMod.CONFIG.crop_growth_time_seconds > 0)) return;
        
        CropBlock crop = (CropBlock) (Object) this;
        int maxAge = crop.getMaxAge();
        int currentAge = crop.getAge(state);
        
        if (currentAge < maxAge) {
            if (random.nextInt(20 * CoreServerMod.CONFIG.crop_growth_time_seconds) == 0) {
                world.setBlockState(pos, crop.withAge(currentAge + 1));
            }
            ci.cancel();
        }
    }
}