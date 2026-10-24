package pro.codeservermod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.SaplingGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.codeservermod.CoreServerMod;

@Mixin(SaplingGenerator.class)
public class SaplingGeneratorMixin {
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void generate(ServerWorld world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state, Random random, CallbackInfoReturnable<Boolean> cir) {
        if (!(CoreServerMod.CONFIG.sapling_growth_time_seconds > 0)) return;
        
        if (random.nextInt(20 * CoreServerMod.CONFIG.sapling_growth_time_seconds) == 0) {
            boolean generated = ((SaplingGenerator) (Object) this).generate(world, chunkGenerator, pos, state, random);
            cir.setReturnValue(generated);
        } else {
            cir.setReturnValue(false);
        }
    }
}