package pro.codeservermod.handler;

import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChickenEggHandler {
    private static final Map<Integer, Long> CHICKEN_EGG_TIMERS = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Setup
    }

    public static void scheduleEggLay(ServerWorld world, int chickenId) {
        long currentTime = world.getTime();
        Long lastLay = CHICKEN_EGG_TIMERS.get(chickenId);
        
        if (lastLay == null) {
            CHICKEN_EGG_TIMERS.put(chickenId, currentTime);
        }
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        int intervalTicks = 20 * pro.codeservermod.CoreServerMod.CONFIG.chicken_egg_interval_seconds;
        
        if (tickCounter % intervalTicks != 0) return;
        
        long currentTime = world.getTime();
        
        for (Map.Entry<Integer, Long> entry : new java.util.HashMap<>(CHICKEN_EGG_TIMERS).entrySet()) {
            int chickenId = entry.getKey();
            long lastLay = entry.getValue();
            
            if (currentTime - lastLay >= intervalTicks) {
                net.minecraft.entity.Entity entity = world.getEntityById(chickenId);
                if (entity instanceof ChickenEntity chicken && !chicken.isBaby()) {
                    ItemStack egg = new ItemStack(Items.EGG, 1);
                    chicken.dropStack(world, egg);
                    CHICKEN_EGG_TIMERS.put(chickenId, currentTime);
                } else if (entity == null || entity instanceof ChickenEntity chicken && chicken.isBaby()) {
                    CHICKEN_EGG_TIMERS.remove(chickenId);
                }
            }
        }
    }

    public static void cleanup() {
        CHICKEN_EGG_TIMERS.clear();
    }
}