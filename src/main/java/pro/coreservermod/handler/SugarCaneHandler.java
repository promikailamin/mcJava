package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import pro.coreservermod.CoreServerMod;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SugarCaneHandler {
    private static final Set<BlockPos> TRACKED_POSITIONS = ConcurrentHashMap.newKeySet();

    public static void register(MinecraftServer server) {
        // Initial scan could be added here if needed
    }

    public static void trackPosition(ServerLevel world, BlockPos pos) {
        if (world.getBlockState(pos.below()).is(Blocks.SOUL_SAND)) {
            TRACKED_POSITIONS.add(pos.immutable());
        }
    }

    public static void untrackPosition(BlockPos pos) {
        TRACKED_POSITIONS.remove(pos);
    }

    public static void tick(ServerLevel world) {
        if (TRACKED_POSITIONS.isEmpty()) return;

        int intervalTicks = 20 * CoreServerMod.CONFIG.sugar_cane_growth_interval_seconds;
        if (world.getGameTime() % intervalTicks != 0) return;

        for (BlockPos pos : new HashSet<>(TRACKED_POSITIONS)) {
            if (!world.isLoaded(pos)) continue;

            if (!world.getBlockState(pos).is(Blocks.SUGAR_CANE)) {
                TRACKED_POSITIONS.remove(pos);
                continue;
            }

            if (!world.getBlockState(pos.below()).is(Blocks.SOUL_SAND)) {
                TRACKED_POSITIONS.remove(pos);
                continue;
            }

            int height = 1;
            while (height < 3 && world.getBlockState(pos.above(height)).is(Blocks.SUGAR_CANE)) {
                height++;
            }

            if (height < 3 && world.getBlockState(pos.above(height)).isAir()) {
                world.setBlockAndUpdate(pos.above(height), Blocks.SUGAR_CANE.defaultBlockState());
            }
        }
    }

    public static void cleanup() {
        TRACKED_POSITIONS.clear();
    }
}
