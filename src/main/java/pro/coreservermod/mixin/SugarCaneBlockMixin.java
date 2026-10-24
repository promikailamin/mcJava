package pro.coreservermod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.coreservermod.CoreServerMod;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void canSurvive(BlockState state, LevelReader world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CoreServerMod.CONFIG.sugar_cane_soul_sand_enabled) return;

        if (world.getBlockState(pos.below()).is(Blocks.SOUL_SAND)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.sugar_cane_soul_sand_enabled) return;

        if (world.getBlockState(pos.below()).is(Blocks.SOUL_SAND)) {
            int maxHeight = 3;
            for (int i = 1; i <= maxHeight; i++) {
                BlockPos abovePos = pos.above(i);
                if (world.getBlockState(abovePos).isAir()) {
                    world.setBlockAndUpdate(abovePos, state);
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
