package pro.coreservermod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.coreservermod.CoreServerMod;
import pro.coreservermod.handler.VillagerEmeraldHandler;

@Mixin(Villager.class)
public class VillagerTradeMixin {
    @Inject(method = "rewardTradeXp", at = @At("TAIL"))
    private void onTrade(MerchantOffer offer, CallbackInfo ci) {
        if (!CoreServerMod.CONFIG.villager_emerald_drop_enabled) return;

        Villager villager = (Villager) (Object) this;
        if (villager.level() instanceof ServerLevel serverLevel) {
            VillagerEmeraldHandler.onTradeCompleted(serverLevel, villager.blockPosition());
        }
    }
}
