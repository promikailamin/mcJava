package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.Vec3;

public class ChickenSpawnHandler {
    public static void register(MinecraftServer server) {
        // Initial setup
    }

    public static void spawnChickenFromEgg(ServerLevel world, BlockPos pos, Vec3 exactPos) {
        Chicken chicken = EntityTypes.CHICKEN.create(world, EntitySpawnReason.TRIGGERED);
        if (chicken != null) {
            chicken.snapTo(exactPos, 0.0F, 0.0F);
            chicken.setPersistenceRequired();
            world.addFreshEntity(chicken);
        }
    }

    public static void cleanup() {
        // Nothing to clean up
    }
}
