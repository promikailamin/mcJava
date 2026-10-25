package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import pro.coreservermod.CoreServerMod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CropGrowthHandler {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> TRACKED_CROPS = new ConcurrentHashMap<>();

    public static void register(MinecraftServer server) {
        // Tracking is populated by block placement and chunk loading.
    }

    private static Map<BlockPos, Long> crops(ServerLevel world) {
        return TRACKED_CROPS.computeIfAbsent(world.dimension(), key -> new ConcurrentHashMap<>());
    }

    public static void scanChunk(ServerLevel world, LevelChunk chunk) {
        chunk.findBlocks(
                state -> state.getBlock() instanceof CropBlock,
                (pos, state) -> crops(world).putIfAbsent(pos.immutable(), world.getGameTime())
        );
    }

    public static void trackCrop(ServerLevel world, BlockPos pos) {
        crops(world).put(pos.immutable(), world.getGameTime());
    }

    public static void untrackCrop(ServerLevel world, BlockPos pos) {
        Map<BlockPos, Long> tracked = TRACKED_CROPS.get(world.dimension());
        if (tracked != null) {
            tracked.remove(pos);
        }
    }

    public static void untrackChunk(ServerLevel world, LevelChunk chunk) {
        Map<BlockPos, Long> tracked = TRACKED_CROPS.get(world.dimension());
        if (tracked == null) return;

        tracked.keySet().removeIf(pos -> chunk.getPos().equals(ChunkPos.containing(pos)));
    }

    public static void tick(ServerLevel world) {
        Map<BlockPos, Long> tracked = TRACKED_CROPS.get(world.dimension());
        if (tracked == null || tracked.isEmpty()) return;

        int totalTicks = 20 * CoreServerMod.CONFIG.crop_growth_time_seconds;
        long currentTime = world.getGameTime();

        for (Map.Entry<BlockPos, Long> entry : tracked.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!world.isLoaded(pos)) continue;

            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) {
                tracked.remove(pos);
                continue;
            }

            int maxAge = crop.getMaxAge();
            int currentAge = crop.getAge(state);
            if (currentAge >= maxAge) {
                tracked.remove(pos);
                continue;
            }

            long elapsed = currentTime - entry.getValue();
            int targetAge = (int) Math.min(maxAge, elapsed * maxAge / totalTicks);
            if (targetAge > currentAge) {
                world.setBlockAndUpdate(pos, crop.getStateForAge(targetAge));
            }
        }
    }

    public static void cleanup() {
        TRACKED_CROPS.clear();
    }
}
