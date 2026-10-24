package pro.codeservermod.mixin;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.codeservermod.CoreServerMod;
import pro.codeservermod.handler.VillagerEmeraldHandler;

@Mixin(VillagerEntity.class)
public class VillagerTradeMixin {
    @Inject(method = "restockAndUpdateDemandBonus", at = @At("TAIL"))
    private void onTrade(CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.villager_emerald_drop_enabled) return;
        
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (villager.getWorld() instanceof ServerWorld serverWorld) {
            VillagerEmeraldHandler.onTradeCompleted(serverWorld, villager.getBlockPos());
        }
    }
}