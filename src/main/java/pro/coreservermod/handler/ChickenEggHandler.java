package pro.coreservermod.handler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pro.coreservermod.CoreServerMod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChickenEggHandler {
    private static final Map<Integer, Long> CHICKEN_EGG_TIMERS = new ConcurrentHashMap<>();

    public static void register(MinecraftServer server) {
        // Initial setup
    }

    public static void scheduleEggLay(ServerLevel world, int chickenId) {
        CHICKEN_EGG_TIMERS.putIfAbsent(chickenId, world.getGameTime());
    }

    public static void tick(ServerLevel world) {
        int intervalTicks = 20 * CoreServerMod.CONFIG.chicken_egg_interval_seconds;
        if (world.getGameTime() % intervalTicks != 0) return;

        long currentTime = world.getGameTime();

        for (Map.Entry<Integer, Long> entry : new java.util.HashMap<>(CHICKEN_EGG_TIMERS).entrySet()) {
            int chickenId = entry.getKey();
            long lastLay = entry.getValue();

            if (currentTime - lastLay >= intervalTicks) {
                Entity entity = world.getEntity(chickenId);
                if (entity instanceof Chicken chicken && !chicken.isBaby()) {
                    ItemStack egg = new ItemStack(Items.EGG, 1);
                    chicken.spawnAtLocation(world, egg);
                    CHICKEN_EGG_TIMERS.put(chickenId, currentTime);
                } else if (entity == null || entity instanceof Chicken chicken && chicken.isBaby()) {
                    CHICKEN_EGG_TIMERS.remove(chickenId);
                }
            }
        }
    }

    public static void cleanup() {
        CHICKEN_EGG_TIMERS.clear();
    }
}
