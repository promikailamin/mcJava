package pro.codeservermod.handler;

import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CropGrowthHandler {
    private static final Set<BlockPos> TRACKED_CROPS = ConcurrentHashMap.newKeySet();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Could do initial scan
    }

    public static void trackCrop(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CropBlock) {
            TRACKED_CROPS.add(pos.toImmutable());
        }
    }

    public static void untrackCrop(BlockPos pos) {
        TRACKED_CROPS.remove(pos);
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        int intervalTicks = 20 * pro.codeservermod.CoreServerMod.CONFIG.crop_growth_time_seconds;
        
        if (tickCounter % intervalTicks != 0) return;
        
        for (BlockPos pos : new java.util.HashSet<>(TRACKED_CROPS)) {
            if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) {
                TRACKED_CROPS.remove(pos);
                continue;
            }
            
            int maxAge = crop.getMaxAge();
            int currentAge = crop.getAge(state);
            
            if (currentAge < maxAge) {
                world.setBlockState(pos, crop.withAge(currentAge + 1));
            } else {
                TRACKED_CROPS.remove(pos);
            }
        }
    }

    public static void cleanup() {
        TRACKED_CROPS.clear();
    }
}