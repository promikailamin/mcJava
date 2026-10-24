package pro.codeservermod.handler;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.SpawnReason;

public class ChickenSpawnHandler {
    public static void register(net.minecraft.server.MinecraftServer server) {
        // Setup
    }

    public static void spawnChickenFromEgg(ServerWorld world, BlockPos pos, Vec3d exactPos) {
        ChickenEntity chicken = EntityType.CHICKEN.create(world, SpawnReason.TRIGGERED);
        if (chicken != null) {
            chicken.refreshPositionAndAngles(exactPos, 0, 0);
            chicken.setPersistent();
            world.spawnEntity(chicken);
        }
    }

    public static void cleanup() {
        // Nothing to clean up
    }
}