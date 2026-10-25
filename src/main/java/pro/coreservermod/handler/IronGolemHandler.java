package pro.coreservermod.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private static final Component CREATE_VILLAGER_NAME = Component.literal(CREATE_VILLAGER_TAG)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    private static final int GOLEM_SEARCH_RADIUS = 16;

    public static void register(MinecraftServer server) {
        // Initial setup
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.iron_golem_spawn_enabled) return;

        long gameTime = world.getGameTime();

        updateCreateVillagers(world);

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
                    playConversionEffects(world, villager);
                    broadcastConversionMessage(world);
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

    private static void playConversionEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        world.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.4F);

        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 30, 0.6, 0.8, 0.6, 0.1);
        world.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 40, 0.8, 1.0, 0.8, 0.4);
        world.sendParticles(ParticleTypes.GLOW, x, y + 1.0, z, 15, 0.5, 0.8, 0.5, 0.05);
    }

    private static void broadcastConversionMessage(ServerLevel world) {
        MinecraftServer server = world.getServer();
        if (server == null) return;

        Component message = Component.literal("A villager has become a ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(CREATE_VILLAGER_TAG).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(message, false);
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
        BlockState below = world.getBlockState(pos.below());
        if (!below.is(Blocks.COBBLESTONE)) return false;

        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.above());

        if (!isAirOrWater(state) || !isAirOrWater(above)) return false;

        AABB box = EntityTypes.IRON_GOLEM.getDimensions()
                .makeBoundingBox(Vec3.atCenterOf(pos));
        return world.noCollision(box);
    }

    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    private static boolean spawnGolem(ServerLevel world, BlockPos villagerPos) {
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
                            playGolemSpawnEffects(world, golem);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void playGolemSpawnEffects(ServerLevel world, IronGolem golem) {
        double x = golem.getX();
        double y = golem.getY();
        double z = golem.getZ();

        world.playSound(null, x, y, z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 1.0F, 0.9F);
        world.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 1.2F);

        world.sendParticles(ParticleTypes.CLOUD, x, y + 1.0, z, 25, 0.7, 0.8, 0.7, 0.08);
        world.sendParticles(ParticleTypes.CRIT, x, y + 1.0, z, 20, 0.6, 0.8, 0.6, 0.2);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1.0, z, 15, 0.5, 0.7, 0.5, 0.1);
    }

    public static void cleanup() {
        // No cached state to clean up.
    }
}
