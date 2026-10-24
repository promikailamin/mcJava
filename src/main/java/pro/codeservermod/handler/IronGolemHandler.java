package pro.codeservermod.handler;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.SpawnReason;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IronGolemHandler {
    private static final Map<BlockPos, Long> VILLAGER_IRON_BLOCK_TIMES = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Initial setup
    }

    public static void scheduleGolemCheck(ServerWorld world, BlockPos villagerPos) {
        // Check if the villager is on an iron block
        if (world.getBlockState(villagerPos.down()).isOf(Blocks.IRON_BLOCK)) {
            VILLAGER_IRON_BLOCK_TIMES.put(villagerPos.toImmutable(), world.getTime());
        }
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        int intervalTicks = 20 * pro.codeservermod.CoreServerMod.CONFIG.iron_golem_spawn_interval_seconds;
        
        if (tickCounter % intervalTicks != 0) return;
        
        if (!pro.codeservermod.CoreServerMod.CONFIG.iron_golem_spawn_enabled) return;

        // Clean up entries for villagers no longer on iron blocks
        for (Map.Entry<BlockPos, Long> entry : new HashMap<>(VILLAGER_IRON_BLOCK_TIMES).entrySet()) {
            BlockPos villagerPos = entry.getKey();
            
            if (!world.isChunkLoaded(villagerPos.getX() >> 4, villagerPos.getZ() >> 4)) continue;
            
            // Check if villager is still on iron block
            if (!world.getBlockState(villagerPos.down()).isOf(Blocks.IRON_BLOCK)) {
                VILLAGER_IRON_BLOCK_TIMES.remove(villagerPos);
                continue;
            }
            
            // Check for valid spawn position
            if (canSpawnGolem(world, villagerPos)) {
                spawnGolem(world, villagerPos);
            }
        }
    }

    private static boolean canSpawnGolem(ServerWorld world, BlockPos villagerPos) {
        // Check if there's a valid spawn position near the villager
        for (int x = -16; x <= 16; x += 4) {
            for (int z = -16; z <= 16; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos checkPos = villagerPos.add(x, y, z);
                    if (isValidSpawnPosition(world, checkPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isValidSpawnPosition(ServerWorld world, BlockPos pos) {
        // Check if position is air and block below is solid
        if (!world.getBlockState(pos).isAir()) return false;
        if (!world.getBlockState(pos.up()).isAir()) return false;
        if (!world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), net.minecraft.util.math.Direction.UP)) return false;
        
        // Check not in liquid
        if (world.getBlockState(pos).getFluidState().isStill()) return false;
        if (world.getBlockState(pos.up()).getFluidState().isStill()) return false;
        
        // Check collision
        Box box = EntityType.IRON_GOLEM.getDimensions().getBoxAt(Vec3d.ofCenter(pos));
        Entity testEntity = EntityType.IRON_GOLEM.create(world, SpawnReason.TRIGGERED);
        if (testEntity == null || !world.isSpaceEmpty(testEntity, box)) return false;
        
        return true;
    }

    private static void spawnGolem(ServerWorld world, BlockPos villagerPos) {
        for (int x = -16; x <= 16; x += 4) {
            for (int z = -16; z <= 16; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos spawnPos = villagerPos.add(x, y, z);
                    if (isValidSpawnPosition(world, spawnPos)) {
                        IronGolemEntity golem = EntityType.IRON_GOLEM.create(world, SpawnReason.TRIGGERED);
                        if (golem != null) {
                            golem.refreshPositionAndAngles(Vec3d.ofCenter(spawnPos), 0, 0);
                            golem.setPersistent();
                            world.spawnEntity(golem);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void cleanup() {
        VILLAGER_IRON_BLOCK_TIMES.clear();
    }
}