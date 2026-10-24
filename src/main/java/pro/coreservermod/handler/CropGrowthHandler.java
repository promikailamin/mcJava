package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import pro.coreservermod.CoreServerMod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CropGrowthHandler {
    private static final Set<BlockPos> TRACKED_CROPS = ConcurrentHashMap.newKeySet();

    public static void register(MinecraftServer server) {
        // Could do initial scan
    }

    public static void trackCrop(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CropBlock) {
            TRACKED_CROPS.add(pos.immutable());
        }
    }

    public static void untrackCrop(BlockPos pos) {
        TRACKED_CROPS.remove(pos);
    }

    public static void tick(ServerLevel world) {
        if (TRACKED_CROPS.isEmpty()) return;

        int intervalTicks = 20 * CoreServerMod.CONFIG.crop_growth_time_seconds;
        if (world.getGameTime() % intervalTicks != 0) return;

        for (BlockPos pos : new java.util.HashSet<>(TRACKED_CROPS)) {
            if (!world.isLoaded(pos)) continue;

            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) {
                TRACKED_CROPS.remove(pos);
                continue;
            }

            int maxAge = crop.getMaxAge();
            int currentAge = crop.getAge(state);

            if (currentAge < maxAge) {
                world.setBlockAndUpdate(pos, crop.getStateForAge(currentAge + 1));
            } else {
                TRACKED_CROPS.remove(pos);
            }
        }
    }

    public static void cleanup() {
        TRACKED_CROPS.clear();
    }
}
