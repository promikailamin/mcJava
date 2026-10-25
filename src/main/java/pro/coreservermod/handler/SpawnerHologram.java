package pro.coreservermod.handler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import pro.coreservermod.mixin.DisplayAccessor;
import pro.coreservermod.mixin.TextDisplayAccessor;

import java.util.List;

public final class SpawnerHologram {
    private static final String TAG_PREFIX = "CoreServerMod_SpawnerHologram_";

    private SpawnerHologram() {
    }

    public static void update(ServerLevel world, Villager villager, String role, Component text) {
        String tag = tagFor(role);
        Display.TextDisplay display = find(villager, tag);

        if (display == null) {
            display = EntityTypes.TEXT_DISPLAY.create(world, EntitySpawnReason.TRIGGERED);
            if (display == null) return;

            display.addTag(tag);
            ((DisplayAccessor) display)
                    .coreservermod$setBillboardConstraints(Display.BillboardConstraints.CENTER);
            ((TextDisplayAccessor) display).coreservermod$setBackgroundColor(0);
            ((TextDisplayAccessor) display).coreservermod$setFlags(Display.TextDisplay.FLAG_SHADOW);
            ((TextDisplayAccessor) display).coreservermod$setText(text);
            world.addFreshEntity(display);
            display.startRiding(villager);
        } else {
            Component currentText = ((TextDisplayAccessor) display).coreservermod$getText();
            if (currentText == null || !text.getString().equals(currentText.getString())) {
                ((TextDisplayAccessor) display).coreservermod$setText(text);
            }
        }

        clearLegacyName(villager, role);
    }

    public static void remove(Villager villager, String role) {
        String tag = tagFor(role);
        for (Entity passenger : List.copyOf(villager.getPassengers())) {
            if (passenger instanceof Display.TextDisplay display
                    && passenger.entityTags().contains(tag)) {
                passenger.stopRiding();
                passenger.discard();
            }
        }
        clearLegacyName(villager, role);
    }

    public static void cleanup(ServerLevel world, String role) {
        String tag = tagFor(role);
        List<? extends Display.TextDisplay> displays = world.getEntities(
                EntityTypes.TEXT_DISPLAY,
                display -> display.entityTags().contains(tag)
        );

        for (Display.TextDisplay display : displays) {
            Entity vehicle = display.getVehicle();
            if (!(vehicle instanceof Villager villager)
                    || !villager.isAlive()
                    || !villager.entityTags().contains(role)) {
                display.stopRiding();
                display.discard();
            }
        }
    }

    private static Display.TextDisplay find(Villager villager, String tag) {
        for (Entity passenger : villager.getPassengers()) {
            if (passenger instanceof Display.TextDisplay display
                    && passenger.entityTags().contains(tag)) {
                return display;
            }
        }
        return null;
    }

    private static String tagFor(String role) {
        return TAG_PREFIX + role;
    }

    private static void clearLegacyName(Villager villager, String role) {
        Component currentName = villager.getCustomName();
        if (currentName == null) return;

        String text = currentName.getString();
        if (text.equals(role) || text.startsWith(role + "\n")) {
            villager.setCustomName(null);
            villager.setCustomNameVisible(false);
        }
    }
}
