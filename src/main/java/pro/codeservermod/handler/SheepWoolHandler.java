package pro.codeservermod.handler;

import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SheepWoolHandler {
    private static final Map<Integer, Long> SHEARED_SHEEP = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Setup
    }

    public static void scheduleRegrowth(ServerWorld world, int sheepId) {
        SHEARED_SHEEP.put(sheepId, world.getTime());
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        int intervalTicks = 20 * pro.codeservermod.CoreServerMod.CONFIG.sheep_wool_regrowth_seconds;
        
        if (tickCounter % intervalTicks != 0) return;
        
        long currentTime = world.getTime();
        
        for (Map.Entry<Integer, Long> entry : new java.util.HashMap<>(SHEARED_SHEEP).entrySet()) {
            int sheepId = entry.getKey();
            long shearTime = entry.getValue();
            
            if (currentTime - shearTime >= intervalTicks) {
                net.minecraft.entity.Entity entity = world.getEntityById(sheepId);
                if (entity instanceof SheepEntity sheep) {
                    if (sheep.isSheared()) {
                        sheep.setSheared(false);
                        SHEARED_SHEEP.remove(sheepId);
                    }
                } else {
                    SHEARED_SHEEP.remove(sheepId);
                }
            }
        }
    }

    public static void cleanup() {
        SHEARED_SHEEP.clear();
    }
}