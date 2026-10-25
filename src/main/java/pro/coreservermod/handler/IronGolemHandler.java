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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IronGolemHandler {
    public static final String CREATE_VILLAGER_TAG = "CreateVillager";
    private static final int GOLEM_SEARCH_RADIUS = SpawnerSupport.SEARCH_RADIUS;
    private static final Map<UUID, BlockPos> SPAWN_AREAS = new HashMap<>();

    public static void register(MinecraftServer server) {
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.iron_golem_spawn_enabled) return;

        long gameTime = world.getGameTime();
        int intervalTicks = Math.max(1, 20 * CoreServerMod.CONFIG.iron_golem_spawn_interval_seconds);
        boolean spawnTick = gameTime % intervalTicks == 0;

        if (gameTime % 20 == 0) {
            SpawnerHologram.cleanup(world, CREATE_VILLAGER_TAG);
        }

        updateCreateVillagers(world, gameTime, intervalTicks, spawnTick);
    }

    private static void updateCreateVillagers(ServerLevel world, long gameTime, int intervalTicks,
                                              boolean spawnTick) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onIronBlock = isStandingOnIronBlock(world, villager);
            boolean hasTag = villager.entityTags().contains(CREATE_VILLAGER_TAG);

            if (!onIronBlock) {
                if (hasTag) {
                    villager.removeTag(CREATE_VILLAGER_TAG);
                    SpawnerHologram.remove(villager, CREATE_VILLAGER_TAG);
                    if (SPAWN_AREAS.remove(villager.getUUID()) != null) {
                        broadcastAreaLost(world);
                    }
                }
                continue;
            }

            boolean newlyConverted = !hasTag;
            if (newlyConverted) {
                villager.addTag(CREATE_VILLAGER_TAG);
                playConversionEffects(world, villager);
                broadcastConversionMessage(world);
            }

            updateSpawnArea(world, villager, gameTime, newlyConverted);
            updateHologram(world, villager, gameTime, intervalTicks,
                    SPAWN_AREAS.containsKey(villager.getUUID()));

            if (spawnTick) {
                trySpawnGolem(world, villager);
            }
        }
    }

    private static void updateSpawnArea(ServerLevel world, Villager villager, long gameTime,
                                        boolean forceSearch) {
        UUID id = villager.getUUID();
        BlockPos cached = SPAWN_AREAS.get(id);
        boolean lost = false;

        if (cached != null && !isValidSpawnPosition(world, cached)) {
            SPAWN_AREAS.remove(id);
            cached = null;
            lost = true;
        }

        if (cached == null && (forceSearch || lost || gameTime % SpawnerSupport.SEARCH_RETRY_TICKS == 0)) {
            BlockPos found = SpawnerSupport.findNearestSpawnPosition(world, villager.blockPosition(),
                    pos -> !pos.equals(villager.blockPosition()) && isValidSpawnPosition(world, pos));

            if (lost) {
                broadcastAreaLost(world);
            }
            if (found != null) {
                SPAWN_AREAS.put(id, found);
                broadcastAreaFound(world, found);
            }
        }
    }

    private static void updateHologram(ServerLevel world, Villager villager, long gameTime,
                                       int intervalTicks, boolean hasSpawnArea) {
        int remainingTicks = intervalTicks - (int) (gameTime % intervalTicks);
        int seconds = Math.max(1, (remainingTicks + 19) / 20);
        String status = hasSpawnArea
                ? "Spawning in " + seconds + (seconds == 1 ? " second!" : " seconds!")
                : "Searching for spawn area...";

        Component hologram = Component.literal(CREATE_VILLAGER_TAG)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\n" + status)
                        .withStyle(hasSpawnArea ? ChatFormatting.YELLOW : ChatFormatting.RED));

        SpawnerHologram.update(world, villager, CREATE_VILLAGER_TAG, hologram);
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
        broadcast(world, Component.literal("A villager has become a ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(CREATE_VILLAGER_TAG).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)));
    }

    private static void broadcastAreaFound(ServerLevel world, BlockPos pos) {
        broadcast(world, Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(CREATE_VILLAGER_TAG).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("] I found my spawn area at ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)));
    }

    private static void broadcastAreaLost(ServerLevel world) {
        broadcast(world, Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(CREATE_VILLAGER_TAG).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("] I lost my spawn area!").withStyle(ChatFormatting.RED)));
    }

    private static void broadcast(ServerLevel world, Component message) {
        MinecraftServer server = world.getServer();
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void trySpawnGolem(ServerLevel world, Villager villager) {
        if (hasReachedGolemLimit(world, villager.blockPosition())) return;

        BlockPos spawnPos = SpawnerSupport.findNearestSpawnPosition(world, villager.blockPosition(),
                pos -> !pos.equals(villager.blockPosition()) && canSpawnGolemAt(world, pos));
        if (spawnPos == null) return;

        IronGolem golem = EntityTypes.IRON_GOLEM.create(world, EntitySpawnReason.TRIGGERED);
        if (golem == null) return;

        golem.snapTo(Vec3.atBottomCenterOf(spawnPos), 0.0F, 0.0F);
        golem.setPersistenceRequired();
        world.addFreshEntity(golem);
        playGolemSpawnEffects(world, golem);
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

    private static boolean canSpawnGolemAt(ServerLevel world, BlockPos pos) {
        if (!isValidSpawnPosition(world, pos)) return false;
        return SpawnerSupport.hasNoEntity(world, golemBox(pos));
    }

    private static boolean isValidSpawnPosition(ServerLevel world, BlockPos pos) {
        if (!world.isLoaded(pos) || !world.isLoaded(pos.below())) return false;

        BlockState below = world.getBlockState(pos.below());
        if (!isValidGolemBase(below)) return false;

        return SpawnerSupport.hasClearSpace(world, pos, golemBox(pos));
    }

    private static AABB golemBox(BlockPos pos) {
        return EntityTypes.IRON_GOLEM.getDimensions()
                .makeBoundingBox(Vec3.atBottomCenterOf(pos));
    }

    private static boolean isValidGolemBase(BlockState state) {
        return state.is(Blocks.COBBLESTONE) || state.is(Blocks.MUD) || state.is(Blocks.HOPPER);
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
        SPAWN_AREAS.clear();
    }
}
