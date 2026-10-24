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
        if (!CoreServerMod.CONFIG.crop_growth_time_seconds > 0) return;
        
        int maxAge = ((CropBlock) (Object) this).getMaxAge();
        int currentAge = state.get(((CropBlock) (Object) this).getAgeProperty());
        
        if (currentAge < maxAge) {
            if (random.nextInt(20 * CoreServerMod.CONFIG.crop_growth_time_seconds) == 0) {
                world.setBlockState(pos, state.cycle(((CropBlock) (Object) this).getAgeProperty()));
            }
            ci.cancel();
        }
    }
}