package pro.codeservermod.handler;

import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SaplingHandler {
    private static final Set<BlockPos> TRACKED_SAPLINGS = ConcurrentHashMap.newKeySet();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Could do initial scan here
    }

    public static void trackSapling(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() instanceof SaplingBlock) {
            TRACKED_SAPLINGS.add(pos.toImmutable());
        }
    }

    public static void untrackSapling(BlockPos pos) {
        TRACKED_SAPLINGS.remove(pos);
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        int intervalTicks = 20 * pro.codeservermod.CoreServerMod.CONFIG.sapling_growth_time_seconds;
        
        if (tickCounter % intervalTicks != 0) return;
        
        for (BlockPos pos : new java.util.HashSet<>(TRACKED_SAPLINGS)) {
            if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            
            if (!(world.getBlockState(pos).getBlock() instanceof SaplingBlock)) {
                TRACKED_SAPLINGS.remove(pos);
                continue;
            }
            
            // Let the vanilla generator handle it with our mixin accelerating the chance
            // The mixin will handle the actual generation
        }
    }

    public static void cleanup() {
        TRACKED_SAPLINGS.clear();
    }
}