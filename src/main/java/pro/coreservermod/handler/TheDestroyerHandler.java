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

import java.util.List;

public class TheDestroyerHandler {
    public static final String THE_DESTROYER_TAG = "TheDestroyer";
    private static final Component THE_DESTROYER_NAME = Component.literal(THE_DESTROYER_TAG)
            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    private static final int CREEPER_SEARCH_RADIUS = 16;

    public static void register(MinecraftServer server) {
    }

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.destroyer_creeper_spawn_enabled) return;

        long gameTime = world.getGameTime();

        updateDestroyers(world);

        int intervalTicks = 20 * CoreServerMod.CONFIG.destroyer_creeper_spawn_interval_seconds;
        if (gameTime % intervalTicks == 0) {
            spawnCreepersForDestroyers(world);
        }
    }

    private static void updateDestroyers(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onTnt = isStandingOnTnt(world, villager);
            boolean hasTag = villager.entityTags().contains(THE_DESTROYER_TAG);

            if (onTnt) {
                if (!hasTag) {
                    villager.addTag(THE_DESTROYER_TAG);
                    playConversionEffects(world, villager);
                    broadcastConversionMessage(world);
                }
                if (!THE_DESTROYER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(THE_DESTROYER_NAME);
                    villager.setCustomNameVisible(true);
                }
            } else if (hasTag) {
                villager.removeTag(THE_DESTROYER_TAG);
                if (THE_DESTROYER_NAME.equals(villager.getCustomName())) {
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

        world.playSound(null, x, y, z, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.7F);
        world.playSound(null, x, y, z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.8F, 1.0F);

        world.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 1.0, z, 30, 0.6, 0.8, 0.6, 0.1);
        world.sendParticles(ParticleTypes.SMOKE, x, y + 1.0, z, 40, 0.7, 0.9, 0.7, 0.05);
        world.sendParticles(ParticleTypes.FLAME, x, y + 1.0, z, 20, 0.5, 0.8, 0.5, 0.05);
    }

    private static void broadcastConversionMessage(ServerLevel world) {
        MinecraftServer server = world.getServer();
        if (server == null) return;

        Component message = Component.literal("A villager has become ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(THE_DESTROYER_TAG).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void spawnCreepersForDestroyers(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            if (!villager.entityTags().contains(THE_DESTROYER_TAG)) continue;
            if (!isStandingOnTnt(world, villager)) continue;
            if (hasReachedCreeperLimit(world, villager.blockPosition())) continue;
            if (canSpawnCreeper(world, villager.blockPosition())) {
                spawnCreeper(world, villager.blockPosition());
            }
        }
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

    private static boolean canSpawnCreeper(ServerLevel world, BlockPos villagerPos) {
        for (int x = -CREEPER_SEARCH_RADIUS; x <= CREEPER_SEARCH_RADIUS; x += 4) {
            for (int z = -CREEPER_SEARCH_RADIUS; z <= CREEPER_SEARCH_RADIUS; z += 4) {
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
        if (!isValidCreeperBase(below)) return false;

        BlockState state = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.above());

        if (!isAirOrWater(state) || !isAirOrWater(above)) return false;

        AABB box = EntityTypes.CREEPER.getDimensions()
                .makeBoundingBox(Vec3.atCenterOf(pos));
        return world.noCollision(box);
    }

    private static boolean isValidCreeperBase(BlockState state) {
        return state.is(Blocks.COBBLESTONE) || state.is(Blocks.MUD) || state.is(Blocks.HOPPER);
    }

    private static boolean isAirOrWater(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    private static boolean spawnCreeper(ServerLevel world, BlockPos villagerPos) {
        for (int x = -CREEPER_SEARCH_RADIUS; x <= CREEPER_SEARCH_RADIUS; x += 4) {
            for (int z = -CREEPER_SEARCH_RADIUS; z <= CREEPER_SEARCH_RADIUS; z += 4) {
                for (int y = -4; y <= 4; y++) {
                    BlockPos spawnPos = villagerPos.offset(x, y, z);
                    if (isValidSpawnPosition(world, spawnPos)) {
                        Creeper creeper = EntityTypes.CREEPER.create(world, EntitySpawnReason.TRIGGERED);
                        if (creeper != null) {
                            creeper.snapTo(Vec3.atCenterOf(spawnPos), 0.0F, 0.0F);
                            creeper.setPersistenceRequired();
                            world.addFreshEntity(creeper);
                            playCreeperSpawnEffects(world, creeper);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
    }
}
