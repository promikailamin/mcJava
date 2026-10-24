package pro.codeservermod.handler;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.Village;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IronGolemHandler {
    private static final Map<BlockPos, Long> VILLAGER_WAKE_TIMES = new ConcurrentHashMap<>();
    private static final Map<Village, Integer> GOLEM_COUNTS = new ConcurrentHashMap<>();
    private static final Set<Village> PROCESSED_VILLAGES = ConcurrentHashMap.newKeySet();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Initial setup
    }

    public static void scheduleGolemCheck(ServerWorld world, BlockPos villagerPos) {
        VILLAGER_WAKE_TIMES.put(villagerPos.toImmutable(), world.getTime());
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        int intervalTicks = 20 * pro.codeservermod.CoreServerMod.CONFIG.iron_golem_spawn_interval_seconds;
        
        if (tickCounter % intervalTicks != 0) return;
        
        if (!pro.codeservermod.CoreServerMod.CONFIG.iron_golem_spawn_enabled) return;

        for (Map.Entry<BlockPos, Long> entry : new HashMap<>(VILLAGER_WAKE_TIMES).entrySet()) {
            BlockPos villagerPos = entry.getKey();
            long wakeTime = entry.getValue();
            
            if (!world.isChunkLoaded(villagerPos.getX() >> 4, villagerPos.getZ() >> 4)) continue;
            
            Village village = world.getVillageCollection().getVillage(villagerPos);
            if (village == null) {
                VILLAGER_WAKE_TIMES.remove(villagerPos);
                continue;
            }
            
            int currentGolems = countGolemsInVillage(world, village);
            int maxGolems = pro.codeservermod.CoreServerMod.CONFIG.max_automatic_golems_per_area;
            
            if (currentGolems >= maxGolems) {
                continue;
            }
            
            if (canSpawnGolem(world, village, villagerPos)) {
                spawnGolem(world, village, villagerPos);
            }
        }
    }

    private static int countGolemsInVillage(ServerWorld world, Village village) {
        Box bounds = new Box(village.getCenter()).expand(64);
        return world.getEntitiesByType(EntityType.IRON_GOLEM, bounds, e -> true).size();
    }

    private static boolean canSpawnGolem(ServerWorld world, Village village, BlockPos villagerPos) {
        // Check if there's a valid spawn position near the village center
        BlockPos center = village.getCenter();
        
        for (int x = -16; x <= 16; x += 4) {
            for (int z = -16; z <= 16; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos checkPos = center.add(x, y, z);
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
        
        return true;
    }

    private static void spawnGolem(ServerWorld world, Village village, BlockPos villagerPos) {
        BlockPos center = village.getCenter();
        
        for (int x = -16; x <= 16; x += 4) {
            for (int z = -16; z <= 16; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos spawnPos = center.add(x, y, z);
                    if (isValidSpawnPosition(world, spawnPos)) {
                        IronGolemEntity golem = EntityType.IRON_GOLEM.create(world);
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
        VILLAGER_WAKE_TIMES.clear();
        GOLEM_COUNTS.clear();
        PROCESSED_VILLAGES.clear();
    }
}