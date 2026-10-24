package pro.coreservermod.handler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import pro.coreservermod.CoreServerMod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SheepWoolHandler {
    private static final Map<Integer, Long> SHEARED_SHEEP = new ConcurrentHashMap<>();

    public static void register(MinecraftServer server) {
        // Initial setup
    }

    public static void scheduleRegrowth(ServerLevel world, int sheepId) {
        SHEARED_SHEEP.put(sheepId, world.getGameTime());
    }

    public static void tick(ServerLevel world) {
        int intervalTicks = 20 * CoreServerMod.CONFIG.sheep_wool_regrowth_seconds;
        if (world.getGameTime() % intervalTicks != 0) return;

        long currentTime = world.getGameTime();

        for (Map.Entry<Integer, Long> entry : new java.util.HashMap<>(SHEARED_SHEEP).entrySet()) {
            int sheepId = entry.getKey();
            long shearTime = entry.getValue();

            if (currentTime - shearTime >= intervalTicks) {
                Entity entity = world.getEntity(sheepId);
                if (entity instanceof Sheep sheep) {
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
