package pro.coreservermod.handler;

import com.zurrtum.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import pro.coreservermod.CoreServerMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CreateFlowingWaterWashingHandler {
    private static final Map<ResourceKey<Level>, Map<UUID, Long>> WASH_TIMERS = new ConcurrentHashMap<>();

    public static void tick(ServerLevel world) {
        if (!CoreServerMod.CONFIG.create_flowing_water_washing_enabled) {
            WASH_TIMERS.remove(world.dimension());
            return;
        }

        Map<UUID, Long> timers = WASH_TIMERS.computeIfAbsent(
                world.dimension(),
                key -> new HashMap<>()
        );

        long gameTime = world.getGameTime();
        int intervalTicks = Math.max(1, CoreServerMod.CONFIG.create_flowing_water_washing_interval_ticks);
        List<? extends ItemEntity> items = world.getEntities(EntityTypes.ITEM, item -> !item.getItem().isEmpty());
        Set<UUID> activeItems = new HashSet<>();

        for (ItemEntity item : items) {
            UUID id = item.getUUID();
            activeItems.add(id);

            if (!isInFlowingWater(world, item)
                    || !AllFanProcessingTypes.SPLASHING.canProcess(item.getItem(), world)) {
                timers.remove(id);
                continue;
            }

            Long readyAt = timers.get(id);
            if (readyAt == null) {
                timers.put(id, gameTime + intervalTicks);
                continue;
            }
            if (gameTime < readyAt) continue;

            wash(world, item);
            timers.remove(id);
        }

        timers.keySet().retainAll(activeItems);
    }

    private static void wash(ServerLevel world, ItemEntity item) {
        List<ItemStack> results = AllFanProcessingTypes.SPLASHING.process(item.getItem().copy(), world);
        if (results == null) return;

        List<ItemStack> outputs = new ArrayList<>(results);
        if (outputs.isEmpty()) {
            item.discard();
            return;
        }

        item.setItem(outputs.removeFirst());
        for (ItemStack output : outputs) {
            ItemEntity spawned = new ItemEntity(
                    world,
                    item.getX(),
                    item.getY(),
                    item.getZ(),
                    output
            );
            spawned.setDeltaMovement(item.getDeltaMovement());
            world.addFreshEntity(spawned);
        }

        AllFanProcessingTypes.SPLASHING.spawnProcessingParticles(world, item.position());
    }

    private static boolean isInFlowingWater(ServerLevel world, ItemEntity item) {
        BlockPos pos = item.blockPosition();
        if (isFlowingWater(world.getFluidState(pos)) || isFlowingWater(world.getFluidState(pos.below()))) {
            return true;
        }

        AABB box = item.getBoundingBox();
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isFlowingWater(world.getFluidState(new BlockPos(x, y, z)))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isFlowingWater(FluidState state) {
        return !state.isEmpty() && state.is(FluidTags.WATER) && !state.isSource();
    }

    public static void cleanup() {
        WASH_TIMERS.clear();
    }
}
