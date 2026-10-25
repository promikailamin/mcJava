package pro.coreservermod.handler;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BadVillagerHandler {
    public static final String BAD_VILLAGER_TAG = "BadVillager";
    private static final Component BAD_VILLAGER_NAME = Component.literal(BAD_VILLAGER_TAG)
            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
    private static final int BAD_OMEN_DURATION_TICKS = 20 * 60 * 20;
    private static final int BAD_OMEN_MAX_AMPLIFIER = 4;

    private static boolean damageEventRegistered;

    public static void register(MinecraftServer server) {
        if (damageEventRegistered) return;
        damageEventRegistered = true;
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(BadVillagerHandler::allowDamage);
    }

    public static void tick(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onRedstoneBlock = isStandingOnRedstoneBlock(world, villager);
            boolean hasTag = villager.entityTags().contains(BAD_VILLAGER_TAG);

            if (onRedstoneBlock) {
                if (!hasTag) {
                    villager.addTag(BAD_VILLAGER_TAG);
                    playConversionEffects(world, villager);
                    broadcastConversionMessage(world);
                }
                if (!BAD_VILLAGER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(BAD_VILLAGER_NAME);
                    villager.setCustomNameVisible(true);
                }
            } else if (hasTag) {
                villager.removeTag(BAD_VILLAGER_TAG);
                if (BAD_VILLAGER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(null);
                    villager.setCustomNameVisible(false);
                }
            }
        }
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Villager villager)) return true;
        if (!villager.entityTags().contains(BAD_VILLAGER_TAG)) return true;

        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof LivingEntity attacker) {
            int amplifier = villager.getRandom().nextInt(BAD_OMEN_MAX_AMPLIFIER + 1);
            attacker.addEffect(
                    new MobEffectInstance(MobEffects.BAD_OMEN, BAD_OMEN_DURATION_TICKS, amplifier),
                    villager
            );
            playBadOmenEffects(attacker);
        }

        return false;
    }

    private static void playConversionEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 0.8F);
        world.playSound(null, x, y, z, SoundEvents.APPLY_EFFECT_BAD_OMEN, SoundSource.PLAYERS, 1.0F, 1.0F);

        world.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 1.0, z, 30, 0.6, 0.8, 0.6, 0.1);
        world.sendParticles(ParticleTypes.SMOKE, x, y + 1.0, z, 30, 0.6, 0.8, 0.6, 0.05);
        world.sendParticles(ParticleTypes.RAID_OMEN, x, y + 1.0, z, 15, 0.5, 0.8, 0.5, 0.05);
    }

    private static void playBadOmenEffects(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel world)) return;

        double x = attacker.getX();
        double y = attacker.getY();
        double z = attacker.getZ();

        world.playSound(null, x, y, z, SoundEvents.APPLY_EFFECT_BAD_OMEN, SoundSource.PLAYERS, 1.0F, 0.9F);
        world.sendParticles(ParticleTypes.RAID_OMEN, x, y + 1.0, z, 20, 0.5, 0.8, 0.5, 0.05);
    }

    private static void broadcastConversionMessage(ServerLevel world) {
        MinecraftServer server = world.getServer();
        if (server == null) return;

        Component message = Component.literal("A villager has become a ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(BAD_VILLAGER_TAG).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static boolean isStandingOnRedstoneBlock(ServerLevel world, Villager villager) {
        BlockPos below = villager.blockPosition().below();
        if (!world.isLoaded(below)) return false;
        BlockState state = world.getBlockState(below);
        return state.is(Blocks.REDSTONE_BLOCK);
    }

    public static void cleanup() {
    }
}
