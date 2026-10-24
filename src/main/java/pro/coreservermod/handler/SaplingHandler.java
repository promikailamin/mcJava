package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SaplingBlock;
import pro.coreservermod.CoreServerMod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SaplingHandler {
    private static final Set<BlockPos> TRACKED_SAPLINGS = ConcurrentHashMap.newKeySet();

    public static void register(MinecraftServer server) {
        // Could do initial scan here
    }

    public static void trackSapling(ServerLevel world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() instanceof SaplingBlock) {
            TRACKED_SAPLINGS.add(pos.immutable());
        }
    }

    public static void untrackSapling(BlockPos pos) {
        TRACKED_SAPLINGS.remove(pos);
    }

    public static void tick(ServerLevel world) {
        if (TRACKED_SAPLINGS.isEmpty()) return;

        int intervalTicks = 20 * CoreServerMod.CONFIG.sapling_growth_time_seconds;
        if (world.getGameTime() % intervalTicks != 0) return;

        for (BlockPos pos : new java.util.HashSet<>(TRACKED_SAPLINGS)) {
            if (!world.isLoaded(pos)) continue;

            if (!(world.getBlockState(pos).getBlock() instanceof SaplingBlock)) {
                TRACKED_SAPLINGS.remove(pos);
            }
        }
    }

    public static void cleanup() {
        TRACKED_SAPLINGS.clear();
    }
}
