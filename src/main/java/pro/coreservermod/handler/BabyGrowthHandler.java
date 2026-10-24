package pro.coreservermod.handler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import pro.coreservermod.CoreServerMod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BabyGrowthHandler {
    private static final Map<Integer, Long> BABY_ENTITIES = new ConcurrentHashMap<>();
    private static final Set<Integer> FED_BABIES = ConcurrentHashMap.newKeySet();

    public static void register(MinecraftServer server) {
        // Initial setup
    }

    public static void trackBaby(ServerLevel world, Entity entity) {
        if (entity instanceof Animal animal && animal.isBaby()) {
            BABY_ENTITIES.putIfAbsent(entity.getId(), world.getGameTime());
        }
    }

    public static void onBabyFed(ServerLevel world, Animal baby, ItemStack stack) {
        if (!CoreServerMod.CONFIG.instant_growth_when_fed) return;
        if (!baby.isBaby()) return;

        if (baby.isFood(stack)) {
            FED_BABIES.add(baby.getId());
            baby.setAge(0);
        }
    }

    public static void tick(ServerLevel world) {
        int intervalTicks = 20 * CoreServerMod.CONFIG.baby_growth_time_seconds;
        if (world.getGameTime() % intervalTicks != 0) return;

        long currentTime = world.getGameTime();
        Set<Integer> presentBabies = new HashSet<>();

        for (Entity entity : world.getAllEntities()) {
            if (!(entity instanceof Animal animal) || !animal.isBaby()) continue;

            int entityId = animal.getId();
            presentBabies.add(entityId);
            long firstSeen = BABY_ENTITIES.computeIfAbsent(entityId, id -> currentTime);

            if (FED_BABIES.remove(entityId) || currentTime - firstSeen >= intervalTicks) {
                animal.setAge(0);
                BABY_ENTITIES.remove(entityId);
            }
        }

        BABY_ENTITIES.keySet().retainAll(presentBabies);
        FED_BABIES.removeIf(id -> !presentBabies.contains(id));
    }

    public static void cleanup() {
        BABY_ENTITIES.clear();
        FED_BABIES.clear();
    }
}
