package pro.coreservermod.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

public final class SpawnerSupport {
    public static final int SEARCH_RADIUS = 16;
    public static final int SEARCH_VERTICAL_RADIUS = 4;
    public static final int SEARCH_RETRY_TICKS = 5;

    private SpawnerSupport() {
    }

    public static BlockPos findNearestSpawnPosition(ServerLevel world, BlockPos center,
                                                    Predicate<BlockPos> validator) {
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                int edge = radius - Math.abs(x);
                if (edge == 0) {
                    BlockPos found = checkColumn(world, center, x, 0, validator);
                    if (found != null) return found;
                } else {
                    BlockPos found = checkColumn(world, center, x, -edge, validator);
                    if (found != null) return found;
                    found = checkColumn(world, center, x, edge, validator);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private static BlockPos checkColumn(ServerLevel world, BlockPos center, int x, int z,
                                        Predicate<BlockPos> validator) {
        for (int offset = 0; offset <= SEARCH_VERTICAL_RADIUS; offset++) {
            if (offset == 0) {
                BlockPos pos = center.offset(x, 0, z);
                if (validator.test(pos)) return pos;
            } else {
                BlockPos below = center.offset(x, -offset, z);
                if (validator.test(below)) return below;
                BlockPos above = center.offset(x, offset, z);
                if (validator.test(above)) return above;
            }
        }
        return null;
    }

    public static boolean hasClearSpace(ServerLevel world, BlockPos pos, AABB box) {
        if (!world.isLoaded(pos)) return false;
        if (!world.noCollision(box)) return false;

        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.ceil(box.maxX) - 1;
        int maxY = (int) Math.ceil(box.maxY) - 1;
        int maxZ = (int) Math.ceil(box.maxZ) - 1;

        for (BlockPos check : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = world.getBlockState(check);
            if (!isAirOrLiquid(state)) return false;
        }
        return true;
    }

    public static boolean hasNoEntity(ServerLevel world, AABB box) {
        return world.getEntitiesOfClass(Entity.class, box, Entity::isAlive).isEmpty();
    }

    public static boolean isAirOrLiquid(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }
}
