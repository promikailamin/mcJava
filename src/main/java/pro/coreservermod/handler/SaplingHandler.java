package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import pro.coreservermod.CoreServerMod;
import pro.coreservermod.mixin.SaplingBlockAccessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SaplingHandler {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> TRACKED_SAPLINGS = new ConcurrentHashMap<>();

    public static void register(MinecraftServer server) {
        // Tracking is populated by block placement and chunk loading.
    }

    private static Map<BlockPos, Long> saplings(ServerLevel world) {
        return TRACKED_SAPLINGS.computeIfAbsent(world.dimension(), key -> new ConcurrentHashMap<>());
    }

    public static void scanChunk(ServerLevel world, LevelChunk chunk) {
        chunk.findBlocks(
                state -> state.getBlock() instanceof SaplingBlock,
                (pos, state) -> saplings(world).putIfAbsent(pos.immutable(), world.getGameTime())
        );
    }

    public static void trackSapling(ServerLevel world, BlockPos pos) {
        saplings(world).put(pos.immutable(), world.getGameTime());
    }

    public static void untrackSapling(ServerLevel world, BlockPos pos) {
        Map<BlockPos, Long> tracked = TRACKED_SAPLINGS.get(world.dimension());
        if (tracked != null) {
            tracked.remove(pos);
        }
    }

    public static void untrackChunk(ServerLevel world, LevelChunk chunk) {
        Map<BlockPos, Long> tracked = TRACKED_SAPLINGS.get(world.dimension());
        if (tracked == null) return;

        tracked.keySet().removeIf(pos -> chunk.getPos().equals(ChunkPos.containing(pos)));
    }

    public static void tick(ServerLevel world) {
        Map<BlockPos, Long> tracked = TRACKED_SAPLINGS.get(world.dimension());
        if (tracked == null || tracked.isEmpty()) return;

        int intervalTicks = 20 * CoreServerMod.CONFIG.sapling_growth_time_seconds;
        long currentTime = world.getGameTime();

        for (Map.Entry<BlockPos, Long> entry : tracked.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!world.isLoaded(pos)) continue;

            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof SaplingBlock sapling)) {
                tracked.remove(pos);
                continue;
            }

            if (state.getBlock() instanceof MangrovePropaguleBlock
                    && state.getValue(MangrovePropaguleBlock.HANGING)) {
                tracked.remove(pos);
                continue;
            }

            if (currentTime - entry.getValue() < intervalTicks) continue;
            if (world.getMaxLocalRawBrightness(pos.above()) < 9) continue;

            TreeGrower grower = ((SaplingBlockAccessor) sapling).coreservermod$getTreeGrower();
            boolean grown = grower.growTree(
                    world,
                    world.getChunkSource().getGenerator(),
                    pos,
                    state,
                    world.getRandom()
            );

            if (grown) {
                tracked.remove(pos);
            } else {
                tracked.put(pos, currentTime);
            }
        }
    }

    public static void cleanup() {
        TRACKED_SAPLINGS.clear();
    }
}
