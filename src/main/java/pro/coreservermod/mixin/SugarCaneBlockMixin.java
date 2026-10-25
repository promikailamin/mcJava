package pro.coreservermod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
