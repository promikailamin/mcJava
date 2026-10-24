package pro.codeservermod.handler;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SugarCaneHandler {
    private static final Set<BlockPos> TRACKED_POSITIONS = ConcurrentHashMap.newKeySet();
    private static final int GROWTH_INTERVAL_TICKS = 20;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Initial scan could be added here if needed
    }

    public static void trackPosition(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos.down()).isOf(Blocks.SOUL_SAND)) {
            TRACKED_POSITIONS.add(pos.toImmutable());
        }
    }

    public static void untrackPosition(BlockPos pos) {
        TRACKED_POSITIONS.remove(pos);
    }

    public static void tick(ServerWorld world) {
        if (TRACKED_POSITIONS.isEmpty()) return;
        
        for (BlockPos pos : new HashSet<>(TRACKED_POSITIONS)) {
            if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            
            if (!world.getBlockState(pos).isOf(Blocks.SUGAR_CANE)) {
                TRACKED_POSITIONS.remove(pos);
                continue;
            }
            
            if (!world.getBlockState(pos.down()).isOf(Blocks.SOUL_SAND)) {
                TRACKED_POSITIONS.remove(pos);
                continue;
            }
            
            int height = 1;
            while (height < 3 && world.getBlockState(pos.up(height)).isOf(Blocks.SUGAR_CANE)) {
                height++;
            }
            
            if (height < 3 && world.getBlockState(pos.up(height)).isAir()) {
                world.setBlockState(pos.up(height), Blocks.SUGAR_CANE.getDefaultState());
            }
        }
    }

    public static void cleanup() {
        TRACKED_POSITIONS.clear();
    }
}