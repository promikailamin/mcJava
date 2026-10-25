package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import pro.coreservermod.CoreServerMod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SugarCaneHandler {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> TRACKED_POSITIONS = new ConcurrentHashMap<>();

    public static void register(MinecraftServer server) {
        // Tracking is populated by block placement and chunk loading.
    }

    private static Map<BlockPos, Long> positions(ServerLevel world) {
        return TRACKED_POSITIONS.computeIfAbsent(world.dimension(), key -> new ConcurrentHashMap<>());
    }

    public static void scanChunk(ServerLevel world, LevelChunk chunk) {
        if (!CoreServerMod.CONFIG.sugar_cane_soul_sand_enabled) return;

        chunk.findBlocks(
                state -> state.is(Blocks.SUGAR_CANE),
                (pos, state) -> {
                    BlockPos basePos = pos.immutable();
                    if (chunk.getBlockState(basePos.below()).is(Blocks.SOUL_SAND)) {
                        positions(world).putIfAbsent(basePos, world.getGameTime());
                    }
                }
        );
    }

    public static void trackPosition(ServerLevel world, BlockPos pos) {
        if (!CoreServerMod.CONFIG.sugar_cane_soul_sand_enabled) return;

        positions(world).put(pos.immutable(), world.getGameTime());
    }

    public static void untrackPosition(ServerLevel world, BlockPos pos) {
        Map<BlockPos, Long> tracked = TRACKED_POSITIONS.get(world.dimension());
        if (tracked != null) {
            tracked.remove(pos);
        }
    }

    public static void untrackChunk(ServerLevel world, LevelChunk chunk) {
        Map<BlockPos, Long> tracked = TRACKED_POSITIONS.get(world.dimension());
        if (tracked == null) return;

        tracked.keySet().removeIf(pos -> chunk.getPos().equals(ChunkPos.containing(pos)));
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.sugar_cane_soul_sand_enabled) return;

        Map<BlockPos, Long> tracked = TRACKED_POSITIONS.get(world.dimension());
        if (tracked == null || tracked.isEmpty()) return;

        int intervalTicks = 20 * CoreServerMod.CONFIG.sugar_cane_growth_interval_seconds;
        long currentTime = world.getGameTime();

        for (Map.Entry<BlockPos, Long> entry : tracked.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!world.isLoaded(pos)) continue;

            if (!world.getBlockState(pos).is(Blocks.SUGAR_CANE)
                    || !world.getBlockState(pos.below()).is(Blocks.SOUL_SAND)) {
                tracked.remove(pos);
                continue;
            }

            if (currentTime - entry.getValue() < intervalTicks) continue;

            int height = 1;
            while (height < 3 && world.getBlockState(pos.above(height)).is(Blocks.SUGAR_CANE)) {
                height++;
            }

            if (height < 3 && world.isEmptyBlock(pos.above(height))) {
                world.setBlockAndUpdate(pos.above(height), Blocks.SUGAR_CANE.defaultBlockState());
            }

            tracked.put(pos, currentTime);
        }
    }

    public static void cleanup() {
        TRACKED_POSITIONS.clear();
    }
}
