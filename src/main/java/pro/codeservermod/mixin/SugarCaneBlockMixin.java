package pro.codeservermod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.codeservermod.Config;
import pro.codeservermod.CoreServerMod;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    private void canPlaceAt(BlockState state, net.minecraft.world.WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CoreServerMod.CONFIG.sugar_cane_soul_sand_enabled) return;
        
        BlockPos downPos = pos.down();
        BlockState downState = world.getBlockState(downPos);
        
        if (downState.isOf(Blocks.SOUL_SAND)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.sugar_cane_soul_sand_enabled) return;
        
        BlockPos downPos = pos.down();
        BlockState downState = world.getBlockState(downPos);
        
        if (downState.isOf(Blocks.SOUL_SAND)) {
            int maxHeight = 3;
            for (int i = 1; i <= maxHeight; i++) {
                BlockPos abovePos = pos.up(i);
                if (world.getBlockState(abovePos).isAir()) {
                    world.setBlockState(abovePos, state);
                    ci.cancel();
                    return;
                }
            }
        }
    }
}