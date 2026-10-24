package pro.codeservermod.handler;

import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimalBreedingHandler {
    private static final Map<Integer, Long> BRED_ENTITIES = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static void register(net.minecraft.server.MinecraftServer server) {
        // Setup
    }

    public static void onAnimalBred(ServerWorld world, AnimalEntity parent1, AnimalEntity parent2) {
        if (!pro.codeservermod.CoreServerMod.CONFIG.animal_breeding_cooldown_disabled) return;
        
        // Reset breeding cooldown immediately
        BRED_ENTITIES.put(parent1.getId(), world.getTime());
        BRED_ENTITIES.put(parent2.getId(), world.getTime());
    }

    public static void tick(ServerWorld world) {
        tickCounter++;
        
        // Clean up old entries
        long currentTime = world.getTime();
        BRED_ENTITIES.entrySet().removeIf(entry -> currentTime - entry.getValue() > 24000); // 20 minutes
    }

    public static void cleanup() {
        BRED_ENTITIES.clear();
    }
}