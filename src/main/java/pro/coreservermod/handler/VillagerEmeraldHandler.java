package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import pro.coreservermod.CoreServerMod;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerEmeraldHandler {
    private static final Map<Integer, Long> TRADE_COOLDOWNS = new ConcurrentHashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    public static void register(MinecraftServer server) {
        // Initial setup
    }

    public static void onTradeCompleted(ServerLevel world, BlockPos villagerPos) {
        if (!CoreServerMod.CONFIG.villager_emerald_drop_enabled) return;

        List<Villager> villagers = world.getEntities(
                EntityTypes.VILLAGER,
                new AABB(villagerPos).inflate(2),
                villager -> villager.blockPosition().equals(villagerPos)
        );

        if (villagers.isEmpty()) return;
        Villager villager = villagers.get(0);

        int villagerId = villager.getId();
        long currentTime = world.getGameTime();

        Long lastTrade = TRADE_COOLDOWNS.get(villagerId);
        if (lastTrade != null && currentTime - lastTrade < 20) {
            return;
        }

        TRADE_COOLDOWNS.put(villagerId, currentTime);

        if (RANDOM.nextDouble() < CoreServerMod.CONFIG.villager_emerald_drop_chance) {
            ItemStack emerald = new ItemStack(Items.EMERALD, 1);
            villager.spawnAtLocation(world, emerald);
        }
    }

    public static void tick(ServerLevel world) {
        if (world.getGameTime() % 20 != 0) return;

        long currentTime = world.getGameTime();
        TRADE_COOLDOWNS.entrySet().removeIf(entry -> currentTime - entry.getValue() > 24000);
    }

    public static void cleanup() {
        TRADE_COOLDOWNS.clear();
    }
}
