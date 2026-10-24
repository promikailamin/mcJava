package pro.coreservermod.handler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class AnimalBreedingHandler {
    public static void register(MinecraftServer server) {
        // Breeding cooldowns are handled by AnimalEntityMixin.
    }

    public static void tick(ServerLevel world) {
        // No per-tick work required.
    }

    public static void cleanup() {
        // Nothing to clean up.
    }
}
