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
import net.minecraft.world.entity.Entity;
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

import java.util.List;

public class TheSlimeSummonerHandler {
    public static final String THE_SLIME_SUMMONER_TAG = "TheSlimeSummoner";
    private static final Component THE_SLIME_SUMMONER_NAME = Component.literal(THE_SLIME_SUMMONER_TAG)
            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
    private static final int SLIME_SEARCH_RADIUS = 16;

    public static void register(MinecraftServer server) {
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.slime_summoner_spawn_enabled) return;

        long gameTime = world.getGameTime();

        updateSlimeSummoners(world);

        int intervalTicks = 20 * CoreServerMod.CONFIG.slime_summoner_spawn_interval_seconds;
        if (gameTime % intervalTicks == 0) {
            summonSlimesForSummoners(world);
        }
    }

    private static void updateSlimeSummoners(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onSlimeBlock = isStandingOnSlimeBlock(world, villager);
            boolean hasTag = villager.entityTags().contains(THE_SLIME_SUMMONER_TAG);

            if (onSlimeBlock) {
                if (!hasTag) {
                    villager.addTag(THE_SLIME_SUMMONER_TAG);
                    playConversionEffects(world, villager);
                    broadcastConversionMessage(world);
                }
                if (!THE_SLIME_SUMMONER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(THE_SLIME_SUMMONER_NAME);
                    villager.setCustomNameVisible(true);
                }
            } else if (hasTag) {
                villager.removeTag(THE_SLIME_SUMMONER_TAG);
                if (THE_SLIME_SUMMONER_NAME.equals(villager.getCustomName())) {
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

        world.playSound(null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.0F, 0.6F);
        world.playSound(null, x, y, z, SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 0.8F, 0.8F);

        world.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 1.0, z, 40, 0.7, 0.9, 0.7, 0.1);
        world.sendParticles(ParticleTypes.CLOUD, x, y + 1.0, z, 20, 0.6, 0.8, 0.6, 0.05);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 15, 0.5, 0.8, 0.5, 0.1);
    }

    private static void broadcastConversionMessage(ServerLevel world) {
        MinecraftServer server = world.getServer();
        if (server == null) return;

        Component message = Component.literal("A villager has become ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(THE_SLIME_SUMMONER_TAG).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void summonSlimesForSummoners(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            if (!villager.entityTags().contains(THE_SLIME_SUMMONER_TAG)) continue;
            if (!isStandingOnSlimeBlock(world, villager)) continue;

            int toSpawn = CoreServerMod.CONFIG.slime_summoner_slimes_per_summon;
            int max = CoreServerMod.CONFIG.max_automatic_slimes_per_area;
            if (max > 0) {
                int nearby = countNearbySlimes(world, villager.blockPosition());
                toSpawn = Math.min(toSpawn, max - nearby);
            }
            if (toSpawn <= 0) continue;

            boolean summoned = false;
            for (int i = 0; i < toSpawn; i++) {
                if (!spawnSlime(world, villager)) break;
                summoned = true;
            }

            if (summoned) {
                playSummonEffects(world, villager);
            }
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
        BlockPos villagerPos = villager.blockPosition();
        var random = villager.getRandom();

        for (int attempt = 0; attempt < 64; attempt++) {
            BlockPos spawnPos = villagerPos.offset(
                    random.nextInt(SLIME_SEARCH_RADIUS * 2 + 1) - SLIME_SEARCH_RADIUS,
                    random.nextInt(9) - 4,
                    random.nextInt(SLIME_SEARCH_RADIUS * 2 + 1) - SLIME_SEARCH_RADIUS
            );
            if (isValidSpawnPosition(world, spawnPos, slime)) {
                spawnSlimeAt(world, slime, spawnPos);
                return true;
            }
        }

        for (int x = -SLIME_SEARCH_RADIUS; x <= SLIME_SEARCH_RADIUS; x += 4) {
            for (int z = -SLIME_SEARCH_RADIUS; z <= SLIME_SEARCH_RADIUS; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos spawnPos = villagerPos.offset(x, y, z);
                    if (isValidSpawnPosition(world, spawnPos, slime)) {
                        spawnSlimeAt(world, slime, spawnPos);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void spawnSlimeAt(ServerLevel world, Slime slime, BlockPos spawnPos) {
        slime.snapTo(Vec3.atCenterOf(spawnPos), 0.0F, 0.0F);
        slime.setPersistenceRequired();
        world.addFreshEntity(slime);
    }

    private static boolean isValidSpawnPosition(ServerLevel world, BlockPos pos, Slime slime) {
        BlockPos belowPos = pos.below();
        BlockState below = world.getBlockState(belowPos);
        if (!below.is(Blocks.SLIME_BLOCK) && !below.isFaceSturdy(world, belowPos, Direction.UP)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.above());

        if (!isAirOrWater(state) || !isAirOrWater(above)) return false;

        AABB box = slime.getDimensions(Pose.STANDING)
                .makeBoundingBox(Vec3.atCenterOf(pos));
        if (!world.noCollision(box)) return false;

        List<Entity> overlapping = world.getEntitiesOfClass(Entity.class, box, Entity::isAlive);
        return overlapping.isEmpty();
    }

    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    private static void playSummonEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.8F, 0.7F);
        world.sendParticles(ParticleTypes.ITEM_SLIME, x, y + 0.6, z, 20, 1.2, 0.5, 1.2, 0.05);
    }

    public static void cleanup() {
    }
}
