package pro.codeservermod.handler;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerEmeraldHandler {
    private static final Map<Integer, Long> TRADE_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Set<Integer> PROCESSED_TRADES = ConcurrentHashMap.newKeySet();
    private static final Random RANDOM = Random.create();

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Setup
    }

    public static void onTradeCompleted(ServerWorld world, BlockPos villagerPos) {
        if (!pro.codeservermod.CoreServerMod.CONFIG.villager_emerald_drop_enabled) return;
        
        // Find villager at position
        VillagerEntity villager = null;
        for (VillagerEntity v : world.getEntitiesByType(net.minecraft.entity.EntityType.VILLAGER, 
                new net.minecraft.util.math.Box(villagerPos).expand(2), e -> true)) {
            if (v.getBlockPos().equals(villagerPos)) {
                villager = v;
                break;
            }
        }
        
        if (villager == null) return;
        
        int villagerId = villager.getId();
        long currentTime = world.getTime();
        
        // Prevent duplicate processing within 1 second (20 ticks)
        Long lastTrade = TRADE_COOLDOWNS.get(villagerId);
        if (lastTrade != null && currentTime - lastTrade < 20) {
            return;
        }
        
        TRADE_COOLDOWNS.put(villagerId, currentTime);
        
        // 40% chance to drop emerald
        if (RANDOM.nextDouble() < pro.codeservermod.CoreServerMod.CONFIG.villager_emerald_drop_chance) {
            ItemStack emerald = new ItemStack(Items.EMERALD, 1);
            villager.dropStack(emerald);
        }
    }

    public static void tick(ServerWorld world) {
        long currentTime = world.getTime();
        TRADE_COOLDOWNS.entrySet().removeIf(entry -> currentTime - entry.getValue() > 24000); // 20 minutes
    }

    public static void cleanup() {
        TRADE_COOLDOWNS.clear();
        PROCESSED_TRADES.clear();
    }
}