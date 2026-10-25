package pro.coreservermod.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.cubemob.Slime;
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

public class TheSlimeSummonerHandler {
    public static final String THE_SLIME_SUMMONER_TAG = "TheSlimeSummoner";
    private static final int SLIME_SEARCH_RADIUS = SpawnerSupport.SEARCH_RADIUS;
    private static final Map<UUID, BlockPos> SPAWN_AREAS = new HashMap<>();

    public static void register(MinecraftServer server) {
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.slime_summoner_spawn_enabled) return;

        long gameTime = world.getGameTime();
        int intervalTicks = Math.max(1, 20 * CoreServerMod.CONFIG.slime_summoner_spawn_interval_seconds);
        boolean spawnTick = gameTime % intervalTicks == 0;

        if (gameTime % 20 == 0) {
            SpawnerHologram.cleanup(world, THE_SLIME_SUMMONER_TAG);
        }

        updateSlimeSummoners(world, gameTime, intervalTicks, spawnTick);
    }

    private static void updateSlimeSummoners(ServerLevel world, long gameTime, int intervalTicks,
                                             boolean spawnTick) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onSlimeBlock = isStandingOnSlimeBlock(world, villager);
            boolean hasTag = villager.entityTags().contains(THE_SLIME_SUMMONER_TAG);

            if (!onSlimeBlock) {
                if (hasTag) {
                    villager.removeTag(THE_SLIME_SUMMONER_TAG);
                    SpawnerHologram.remove(villager, THE_SLIME_SUMMONER_TAG);
                    if (SPAWN_AREAS.remove(villager.getUUID()) != null) {
                        broadcastAreaLost(world);
                    }
                }
                continue;
            }

            boolean newlyConverted = !hasTag;
            if (newlyConverted) {
                villager.addTag(THE_SLIME_SUMMONER_TAG);
                playConversionEffects(world, villager);
                broadcastConversionMessage(world);
            }

            updateSpawnArea(world, villager, gameTime, newlyConverted);
            updateHologram(world, villager, gameTime, intervalTicks,
                    SPAWN_AREAS.containsKey(villager.getUUID()));

            if (spawnTick) {
                summonSlimes(world, villager);
            }
        }
    }

    private static void updateSpawnArea(ServerLevel world, Villager villager, long gameTime,
                                        boolean forceSearch) {
        UUID id = villager.getUUID();
        BlockPos cached = SPAWN_AREAS.get(id);
        boolean lost = false;

        if (cached != null && !isValidSpawnArea(world, cached)) {
            SPAWN_AREAS.remove(id);
            cached = null;
            lost = true;
        }

        if (cached == null && (forceSearch || lost || gameTime % SpawnerSupport.SEARCH_RETRY_TICKS == 0)) {
            BlockPos found = SpawnerSupport.findNearestSpawnPosition(world, villager.blockPosition(),
                    pos -> !pos.equals(villager.blockPosition()) && isValidSpawnArea(world, pos));

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

        Component hologram = Component.literal(THE_SLIME_SUMMONER_TAG)
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal("\n" + status)
                        .withStyle(hasSpawnArea ? ChatFormatting.YELLOW : ChatFormatting.RED));

        SpawnerHologram.update(world, villager, THE_SLIME_SUMMONER_TAG, hologram);
    }

    private static void playConversionEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.0F, 0.6F);
        world.playSound(null, x, y, z, SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 0.8F, 0.8F);

        world.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 1.0, z, 40, 0.7, 0.9, 0.7, 0.1);
        world.sendParticles(ParticleTypes.CLOUD, x, y + 1.0, z, 20, 0.6, 0.8, 0.6, 0.05);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 15, 0.5, 0.8, 0.5, 0.1);
    }

    private static void broadcastConversionMessage(ServerLevel world) {
        broadcast(world, Component.literal("A villager has become ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(THE_SLIME_SUMMONER_TAG)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)));
    }

    private static void broadcastAreaFound(ServerLevel world, BlockPos pos) {
        broadcast(world, Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(THE_SLIME_SUMMONER_TAG)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("] I found my spawn area at ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)));
    }

    private static void broadcastAreaLost(ServerLevel world) {
        broadcast(world, Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(THE_SLIME_SUMMONER_TAG)
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("] I lost my spawn area!").withStyle(ChatFormatting.RED)));
    }

    private static void broadcast(ServerLevel world, Component message) {
        MinecraftServer server = world.getServer();
        if (server == null) return;
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void summonSlimes(ServerLevel world, Villager villager) {
        int toSpawn = CoreServerMod.CONFIG.slime_summoner_slimes_per_summon;
        int max = CoreServerMod.CONFIG.max_automatic_slimes_per_area;
        if (max > 0) {
            int nearby = countNearbySlimes(world, villager.blockPosition());
            toSpawn = Math.min(toSpawn, max - nearby);
        }
        if (toSpawn <= 0) return;

        boolean summoned = false;
        for (int i = 0; i < toSpawn; i++) {
            if (!spawnSlime(world, villager)) break;
            summoned = true;
        }

        if (summoned) {
            playSummonEffects(world, villager);
        }
    }

    private static boolean isStandingOnSlimeBlock(ServerLevel world, Villager villager) {
        BlockPos below = villager.blockPosition().below();
        return world.isLoaded(below) && world.getBlockState(below).is(Blocks.SLIME_BLOCK);
    }

    private static int countNearbySlimes(ServerLevel world, BlockPos villagerPos) {
        List<Slime> nearby = world.getEntities(
                EntityTypes.SLIME,
                new AABB(villagerPos).inflate(SLIME_SEARCH_RADIUS),
                slime -> true
        );
        return nearby.size();
    }

    private static boolean spawnSlime(ServerLevel world, Villager villager) {
        Slime slime = EntityTypes.SLIME.create(world, EntitySpawnReason.TRIGGERED);
        if (slime == null) return false;

        slime.setSize(1 + villager.getRandom().nextInt(2), true);

        BlockPos spawnPos = SpawnerSupport.findNearestSpawnPosition(world, villager.blockPosition(),
                pos -> !pos.equals(villager.blockPosition()) && canSpawnSlimeAt(world, pos, slime));
        if (spawnPos == null) return false;

        slime.snapTo(Vec3.atBottomCenterOf(spawnPos), 0.0F, 0.0F);
        slime.setPersistenceRequired();
        world.addFreshEntity(slime);
        return true;
    }

    private static boolean canSpawnSlimeAt(ServerLevel world, BlockPos pos, Slime slime) {
        if (!isValidSlimeBase(world, pos.below())) return false;
        return SpawnerSupport.hasClearSpace(world, pos, slimeBox(slime, pos))
                && SpawnerSupport.hasNoEntity(world, slimeBox(slime, pos));
    }

    private static boolean isValidSpawnArea(ServerLevel world, BlockPos pos) {
        if (!world.isLoaded(pos) || !world.isLoaded(pos.below())) return false;
        if (!isValidSlimeBase(world, pos.below())) return false;

        AABB box = EntityTypes.SLIME.getDimensions()
                .makeBoundingBox(Vec3.atBottomCenterOf(pos));
        return SpawnerSupport.hasClearSpace(world, pos, box);
    }

    private static boolean isValidSlimeBase(ServerLevel world, BlockPos belowPos) {
        BlockState below = world.getBlockState(belowPos);
        if (below.is(Blocks.SLIME_BLOCK)) return true;
        return below.isFaceSturdy(world, belowPos, Direction.UP)
                && below.isCollisionShapeFullBlock(world, belowPos);
    }

    private static AABB slimeBox(Slime slime, BlockPos pos) {
        return slime.getDimensions(Pose.STANDING)
                .makeBoundingBox(Vec3.atBottomCenterOf(pos));
    }

    private static void playSummonEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.8F, 0.7F);
        world.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 0.6, z, 20, 1.2, 0.5, 1.2, 0.05);
    }

    public static void cleanup() {
        SPAWN_AREAS.clear();
    }
}
