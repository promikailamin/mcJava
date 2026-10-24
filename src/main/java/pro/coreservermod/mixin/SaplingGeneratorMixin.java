package pro.coreservermod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.coreservermod.CoreServerMod;

@Mixin(TreeGrower.class)
public class SaplingGeneratorMixin {
    @Inject(method = "growTree", at = @At("HEAD"), cancellable = true)
    private void growTree(ServerLevel world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state,
                          RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (!(CoreServerMod.CONFIG.sapling_growth_time_seconds > 0)) return;

        if (random.nextInt(20 * CoreServerMod.CONFIG.sapling_growth_time_seconds) != 0) {
            cir.setReturnValue(false);
        }
    }
}
