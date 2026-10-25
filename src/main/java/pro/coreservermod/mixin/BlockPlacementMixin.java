package pro.coreservermod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.coreservermod.CoreServerMod;
import pro.coreservermod.handler.CropGrowthHandler;
import pro.coreservermod.handler.SaplingHandler;
import pro.coreservermod.handler.SugarCaneHandler;

@Mixin(BlockBehaviour.class)
public class BlockPlacementMixin {
    @Inject(method = "onPlace", at = @At("TAIL"))
    private void coreservermod$trackPlacedBlock(BlockState state, Level level, BlockPos pos,
                                                BlockState oldState, boolean movedByPiston,
                                                CallbackInfo ci) {
        if (CoreServerMod.CONFIG == null) return;
        if (!(level instanceof ServerLevel world)) return;
        if (oldState.getBlock() == state.getBlock()) return;

        Block block = state.getBlock();
        if (block instanceof SugarCaneBlock) {
            SugarCaneHandler.trackPosition(world, pos);
        } else if (block instanceof SaplingBlock) {
            SaplingHandler.trackSapling(world, pos);
        } else if (block instanceof CropBlock) {
            CropGrowthHandler.trackCrop(world, pos);
        }
    }
}
