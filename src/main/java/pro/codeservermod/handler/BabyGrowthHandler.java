package pro.codeservermod.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BabyGrowthHandler {
    private static final Map<Integer, Long> BABY_ENTITIES = new ConcurrentHashMap<>();
    private static final Set<Integer> FED_BABIES = ConcurrentHashMap.newKeySet();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Setup
    }

    public static void trackBaby(ServerWorld world, Entity entity) {
        if (entity instanceof AnimalEntity animal && animal.isBaby()) {
            BABY_ENTITIES.put(entity.getId(), world.getTime());
        }
    }

    public static void onBabyFed(ServerWorld world, AnimalEntity baby, ItemStack stack) {
        if (!pro.codeservermod.CoreServerMod.CONFIG.instant_growth_when_fed) return;
        if (!baby.isBaby()) return;
        
        // Check if it's a valid breeding food for this animal
        if (baby.isBreedingItem(stack)) {
            FED_BABIES.add(baby.getId());
            baby.setBaby(false);
            baby.growUp();
        }
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        int intervalTicks = 20 * pro.codeservermod.CoreServerMod.CONFIG.baby_growth_time_seconds;
        
        if (tickCounter % intervalTicks != 0) return;
        
        long currentTime = world.getTime();
        
        for (Map.Entry<Integer, Long> entry : new java.util.HashMap<>(BABY_ENTITIES).entrySet()) {
            int entityId = entry.getKey();
            long spawnTime = entry.getValue();
            
            Entity entity = world.getEntityById(entityId);
            if (entity == null || !(entity instanceof AnimalEntity animal)) {
                BABY_ENTITIES.remove(entityId);
                FED_BABIES.remove(entityId);
                continue;
            }
            
            if (!animal.isBaby()) {
                BABY_ENTITIES.remove(entityId);
                FED_BABIES.remove(entityId);
                continue;
            }
            
            if (FED_BABIES.contains(entityId)) {
                BABY_ENTITIES.remove(entityId);
                FED_BABIES.remove(entityId);
                continue;
            }
            
            if (currentTime - spawnTime >= intervalTicks) {
                animal.setBaby(false);
                animal.growUp();
                BABY_ENTITIES.remove(entityId);
            }
        }
    }

    public static void cleanup() {
        BABY_ENTITIES.clear();
        FED_BABIES.clear();
    }
}