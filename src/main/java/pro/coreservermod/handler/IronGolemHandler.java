package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pro.coreservermod.CoreServerMod;

import java.util.List;

public class IronGolemHandler {
    public static final String CREATE_VILLAGER_TAG = "CreateVillager";
    private static final Component CREATE_VILLAGER_NAME = Component.literal(CREATE_VILLAGER_TAG);
    private static final int VILLAGER_CHECK_INTERVAL_TICKS = 20 * 10;
    private static final int GOLEM_SEARCH_RADIUS = 16;

    public static void register(MinecraftServer server) {
        // Initial setup
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.iron_golem_spawn_enabled) return;

        long gameTime = world.getGameTime();

        if (gameTime % VILLAGER_CHECK_INTERVAL_TICKS == 0) {
            updateCreateVillagers(world);
        }

        int intervalTicks = 20 * CoreServerMod.CONFIG.iron_golem_spawn_interval_seconds;
        if (gameTime % intervalTicks == 0) {
            spawnGolemsForCreateVillagers(world);
        }
    }

    private static void updateCreateVillagers(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onIronBlock = isStandingOnIronBlock(world, villager);
            boolean hasTag = villager.entityTags().contains(CREATE_VILLAGER_TAG);

            if (onIronBlock) {
                if (!hasTag) {
                    villager.addTag(CREATE_VILLAGER_TAG);
                }
                if (!CREATE_VILLAGER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(CREATE_VILLAGER_NAME);
                    villager.setCustomNameVisible(true);
                }
            } else if (hasTag) {
                villager.removeTag(CREATE_VILLAGER_TAG);
                if (CREATE_VILLAGER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(null);
                    villager.setCustomNameVisible(false);
                }
            }
        }
    }

    private static void spawnGolemsForCreateVillagers(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            if (!villager.entityTags().contains(CREATE_VILLAGER_TAG)) continue;
            if (!isStandingOnIronBlock(world, villager)) continue;
            if (hasReachedGolemLimit(world, villager.blockPosition())) continue;
            if (canSpawnGolem(world, villager.blockPosition())) {
                spawnGolem(world, villager.blockPosition());
            }
        }
    }

    private static boolean isStandingOnIronBlock(ServerLevel world, Villager villager) {
        BlockPos below = villager.blockPosition().below();
        return world.isLoaded(below) && world.getBlockState(below).is(Blocks.IRON_BLOCK);
    }

    private static boolean hasReachedGolemLimit(ServerLevel world, BlockPos villagerPos) {
        int max = CoreServerMod.CONFIG.max_automatic_golems_per_area;
        if (max <= 0) return false;

        List<IronGolem> nearby = world.getEntities(
                EntityTypes.IRON_GOLEM,
                new AABB(villagerPos).inflate(GOLEM_SEARCH_RADIUS),
                golem -> true
        );
        return nearby.size() >= max;
    }

    private static boolean canSpawnGolem(ServerLevel world, BlockPos villagerPos) {
        for (int x = -GOLEM_SEARCH_RADIUS; x <= GOLEM_SEARCH_RADIUS; x += 4) {
            for (int z = -GOLEM_SEARCH_RADIUS; z <= GOLEM_SEARCH_RADIUS; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos checkPos = villagerPos.offset(x, y, z);
                    if (isValidSpawnPosition(world, checkPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isValidSpawnPosition(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.above());
        BlockState below = world.getBlockState(pos.below());

        if (!state.isAir() || !above.isAir()) return false;
        if (!below.isFaceSturdy(world, pos.below(), Direction.UP)) return false;

        if (!state.getFluidState().isEmpty() || !above.getFluidState().isEmpty()) return false;

        AABB box = EntityTypes.IRON_GOLEM.getDimensions()
                .makeBoundingBox(Vec3.atCenterOf(pos));
        return world.noCollision(box);
    }

    private static void spawnGolem(ServerLevel world, BlockPos villagerPos) {
        for (int x = -GOLEM_SEARCH_RADIUS; x <= GOLEM_SEARCH_RADIUS; x += 4) {
            for (int z = -GOLEM_SEARCH_RADIUS; z <= GOLEM_SEARCH_RADIUS; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos spawnPos = villagerPos.offset(x, y, z);
                    if (isValidSpawnPosition(world, spawnPos)) {
                        IronGolem golem = EntityTypes.IRON_GOLEM.create(world, EntitySpawnReason.TRIGGERED);
                        if (golem != null) {
                            golem.snapTo(Vec3.atCenterOf(spawnPos), 0.0F, 0.0F);
                            golem.setPersistenceRequired();
                            world.addFreshEntity(golem);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void cleanup() {
        // No cached state to clean up.
    }
}
