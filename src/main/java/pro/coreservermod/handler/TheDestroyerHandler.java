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
import net.minecraft.world.entity.monster.Creeper;
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

public class TheDestroyerHandler {
    public static final String THE_DESTROYER_TAG = "TheDestroyer";
    private static final int CREEPER_SEARCH_RADIUS = SpawnerSupport.SEARCH_RADIUS;
    private static final Map<UUID, BlockPos> SPAWN_AREAS = new HashMap<>();

    public static void register(MinecraftServer server) {
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.destroyer_creeper_spawn_enabled) return;

        long gameTime = world.getGameTime();
        int intervalTicks = Math.max(1, 20 * CoreServerMod.CONFIG.destroyer_creeper_spawn_interval_seconds);
        boolean spawnTick = gameTime % intervalTicks == 0;

        if (gameTime % 20 == 0) {
            SpawnerHologram.cleanup(world, THE_DESTROYER_TAG);
        }

        updateDestroyers(world, gameTime, intervalTicks, spawnTick);
    }

    private static void updateDestroyers(ServerLevel world, long gameTime, int intervalTicks,
                                         boolean spawnTick) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onTnt = isStandingOnTnt(world, villager);
            boolean hasTag = villager.entityTags().contains(THE_DESTROYER_TAG);

            if (!onTnt) {
                if (hasTag) {
                    villager.removeTag(THE_DESTROYER_TAG);
                    SpawnerHologram.remove(villager, THE_DESTROYER_TAG);
                    if (SPAWN_AREAS.remove(villager.getUUID()) != null) {
                        broadcastAreaLost(world);
                    }
                }
                continue;
            }

            boolean newlyConverted = !hasTag;
            if (newlyConverted) {
                villager.addTag(THE_DESTROYER_TAG);
                playConversionEffects(world, villager);
                broadcastConversionMessage(world);
            }

            updateSpawnArea(world, villager, gameTime, newlyConverted);
            updateHologram(world, villager, gameTime, intervalTicks,
                    SPAWN_AREAS.containsKey(villager.getUUID()));

            if (spawnTick) {
                trySpawnCreeper(world, villager);
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

        Component hologram = Component.literal(THE_DESTROYER_TAG)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal("\n" + status)
                        .withStyle(hasSpawnArea ? ChatFormatting.YELLOW : ChatFormatting.RED));

        SpawnerHologram.update(world, villager, THE_DESTROYER_TAG, hologram);
    }

    private static void playConversionEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.7F);
        world.playSound(null, x, y, z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.8F, 1.0F);

        world.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 1.0, z, 30, 0.6, 0.8, 0.6, 0.1);
        world.sendParticles(ParticleTypes.SMOKE, x, y + 1.0, z, 40, 0.7, 0.9, 0.7, 0.05);
        world.sendParticles(ParticleTypes.FLAME, x, y + 1.0, z, 20, 0.5, 0.8, 0.5, 0.05);
    }

    private static void broadcastConversionMessage(ServerLevel world) {
        broadcast(world, Component.literal("A villager has become ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(THE_DESTROYER_TAG).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)));
    }

    private static void broadcastAreaFound(ServerLevel world, BlockPos pos) {
        broadcast(world, Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(THE_DESTROYER_TAG).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("] I found my spawn area at ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)));
    }

    private static void broadcastAreaLost(ServerLevel world) {
        broadcast(world, Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(THE_DESTROYER_TAG).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("] I lost my spawn area!").withStyle(ChatFormatting.RED)));
    }

    private static void broadcast(ServerLevel world, Component message) {
        MinecraftServer server = world.getServer();
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void trySpawnCreeper(ServerLevel world, Villager villager) {
        if (hasReachedCreeperLimit(world, villager.blockPosition())) return;

        BlockPos spawnPos = SpawnerSupport.findNearestSpawnPosition(world, villager.blockPosition(),
                pos -> !pos.equals(villager.blockPosition()) && canSpawnCreeperAt(world, pos));
        if (spawnPos == null) return;

        Creeper creeper = EntityTypes.CREEPER.create(world, EntitySpawnReason.TRIGGERED);
        if (creeper == null) return;

        creeper.snapTo(Vec3.atBottomCenterOf(spawnPos), 0.0F, 0.0F);
        creeper.setPersistenceRequired();
        world.addFreshEntity(creeper);
        playCreeperSpawnEffects(world, creeper);
    }

    private static boolean isStandingOnTnt(ServerLevel world, Villager villager) {
        BlockPos below = villager.blockPosition().below();
        return world.isLoaded(below) && world.getBlockState(below).is(Blocks.TNT);
    }

    private static boolean hasReachedCreeperLimit(ServerLevel world, BlockPos villagerPos) {
        int max = CoreServerMod.CONFIG.max_automatic_creepers_per_area;
        if (max <= 0) return false;

        List<Creeper> nearby = world.getEntities(
                EntityTypes.CREEPER,
                new AABB(villagerPos).inflate(CREEPER_SEARCH_RADIUS),
                creeper -> true
        );
        return nearby.size() >= max;
    }

    private static boolean canSpawnCreeperAt(ServerLevel world, BlockPos pos) {
        if (!isValidSpawnPosition(world, pos)) return false;
        return SpawnerSupport.hasNoEntity(world, creeperBox(pos));
    }

    private static boolean isValidSpawnPosition(ServerLevel world, BlockPos pos) {
        if (!world.isLoaded(pos) || !world.isLoaded(pos.below())) return false;

        BlockState below = world.getBlockState(pos.below());
        if (!isValidCreeperBase(below)) return false;

        return SpawnerSupport.hasClearSpace(world, pos, creeperBox(pos));
    }

    private static AABB creeperBox(BlockPos pos) {
        return EntityTypes.CREEPER.getDimensions()
                .makeBoundingBox(Vec3.atBottomCenterOf(pos));
    }

    private static boolean isValidCreeperBase(BlockState state) {
        return state.is(Blocks.COBBLESTONE) || state.is(Blocks.MUD) || state.is(Blocks.HOPPER);
    }

    private static void playCreeperSpawnEffects(ServerLevel world, Creeper creeper) {
        double x = creeper.getX();
        double y = creeper.getY();
        double z = creeper.getZ();

        world.playSound(null, x, y, z, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 0.6F, 1.2F);
        world.playSound(null, x, y, z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.4F, 1.4F);

        world.sendParticles(ParticleTypes.SMOKE, x, y + 1.0, z, 25, 0.6, 0.8, 0.6, 0.05);
        world.sendParticles(ParticleTypes.FLAME, x, y + 1.0, z, 15, 0.5, 0.7, 0.5, 0.05);
    }

    public static void cleanup() {
        SPAWN_AREAS.clear();
    }
}
