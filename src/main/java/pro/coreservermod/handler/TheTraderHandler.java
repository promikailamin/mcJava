package pro.coreservermod.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Predicate;

public class TheTraderHandler {
    public static final String THE_TRADER_TAG = "TheTrader";
    private static final Component THE_TRADER_NAME = Component.literal(THE_TRADER_TAG)
            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
    private static final double PICKUP_RADIUS = 2.0D;
    private static final int MAX_EMERALD_STACK = 64;

    public static void register(MinecraftServer server) {
    }

    public static void tick(ServerLevel world) {
        for (Villager villager : world.getEntities(EntityTypes.VILLAGER, villager -> true)) {
            boolean onEmeraldBlock = isStandingOnEmeraldBlock(world, villager);
            boolean hasTag = villager.entityTags().contains(THE_TRADER_TAG);
            boolean namedTrader = hasPlayerGivenTraderName(villager);
            boolean isTrader = onEmeraldBlock || namedTrader;

            if (isTrader) {
                if (!hasTag) {
                    villager.addTag(THE_TRADER_TAG);
                    playConversionEffects(world, villager);
                    broadcastConversionMessage(world);
                }
                if (!namedTrader && !THE_TRADER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(THE_TRADER_NAME);
                    villager.setCustomNameVisible(true);
                }
                processNearbyItems(world, villager);
            } else if (hasTag) {
                villager.removeTag(THE_TRADER_TAG);
                if (THE_TRADER_NAME.equals(villager.getCustomName())) {
                    villager.setCustomName(null);
                    villager.setCustomNameVisible(false);
                }
            }
        }
    }

    private static void processNearbyItems(ServerLevel world, Villager villager) {
        AABB pickupArea = villager.getBoundingBox().inflate(PICKUP_RADIUS);
        List<ItemEntity> items = world.getEntities(
                EntityTypes.ITEM,
                pickupArea,
                item -> item.isAlive() && !item.getItem().isEmpty() && !item.getItem().is(Items.EMERALD)
        );
        if (items.isEmpty()) return;

        boolean traded = false;
        traded |= processTrade(world, villager, items, stack -> stack.is(Items.STICK), 8, 1);
        traded |= processTrade(world, villager, items, stack -> stack.is(ItemTags.PLANKS), 4, 1);
        traded |= processTrade(world, villager, items, stack -> stack.is(ItemTags.LOGS), 1, 1);
        traded |= processTrade(world, villager, items, stack -> stack.is(Items.IRON_INGOT), 1, 1);
        traded |= processTrade(world, villager, items, stack -> stack.is(Items.DIAMOND), 1, 9);

        if (traded) {
            playTradeEffects(world, villager);
        }
    }

    private static boolean processTrade(ServerLevel world, Villager villager, List<ItemEntity> items,
                                        Predicate<ItemStack> matches, int requiredItems, int emeraldsPerTrade) {
        int available = 0;
        for (ItemEntity item : items) {
            if (!item.isAlive()) continue;
            ItemStack stack = item.getItem();
            if (matches.test(stack)) {
                available += stack.getCount();
            }
        }

        int trades = available / requiredItems;
        if (trades <= 0) return false;

        int toConsume = trades * requiredItems;
        for (ItemEntity item : items) {
            if (toConsume <= 0) break;
            if (!item.isAlive()) continue;

            ItemStack stack = item.getItem();
            if (!matches.test(stack)) continue;

            int consumed = Math.min(toConsume, stack.getCount());
            stack.shrink(consumed);
            toConsume -= consumed;

            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
        }

        int remainingEmeralds = trades * emeraldsPerTrade;
        while (remainingEmeralds > 0) {
            int amount = Math.min(remainingEmeralds, MAX_EMERALD_STACK);
            villager.spawnAtLocation(world, new ItemStack(Items.EMERALD, amount));
            remainingEmeralds -= amount;
        }

        return true;
    }

    private static void playConversionEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.2F);
        world.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.5F);

        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 30, 0.6, 0.8, 0.6, 0.1);
        world.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 40, 0.8, 1.0, 0.8, 0.4);
        world.sendParticles(ParticleTypes.GLOW, x, y + 1.0, z, 15, 0.5, 0.8, 0.5, 0.05);
    }

    private static void playTradeEffects(ServerLevel world, Villager villager) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        world.playSound(null, x, y, z, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.8F, 1.2F);
        world.playSound(null, x, y, z, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.1F);

        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1.0, z, 15, 0.4, 0.6, 0.4, 0.1);
        world.sendParticles(ParticleTypes.GLOW, x, y + 0.8, z, 10, 0.4, 0.6, 0.4, 0.05);
    }

    private static void broadcastConversionMessage(ServerLevel world) {
        MinecraftServer server = world.getServer();
        if (server == null) return;

        Component message = Component.literal("A villager has become a ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(THE_TRADER_TAG).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static boolean isStandingOnEmeraldBlock(ServerLevel world, Villager villager) {
        BlockPos below = villager.blockPosition().below();
        if (!world.isLoaded(below)) return false;
        BlockState state = world.getBlockState(below);
        return state.is(Blocks.EMERALD_BLOCK);
    }

    private static boolean hasPlayerGivenTraderName(Villager villager) {
        if (!villager.hasCustomName()) return false;
        Component name = villager.getCustomName();
        if (THE_TRADER_NAME.equals(name)) return false;
        return THE_TRADER_TAG.equals(name.getString());
    }

    public static void cleanup() {
    }
}
