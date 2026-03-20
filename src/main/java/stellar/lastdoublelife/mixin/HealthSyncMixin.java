package stellar.lastdoublelife.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stellar.lastdoublelife.config.ModConfig;
import stellar.lastdoublelife.data.SoulDuo;
import stellar.lastdoublelife.manager.GameManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(LivingEntity.class)
public abstract class HealthSyncMixin {

    private static final Set<UUID> SYNCING = ConcurrentHashMap.newKeySet();

    @Inject(method = "setHealth", at = @At("TAIL"))
    private void onSetHealth(float health, CallbackInfo ci) {
        if (!ModConfig.get().syncHealth) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;
        if (self.level().isClientSide()) return;

        UUID uuid = player.getUUID();
        if (SYNCING.contains(uuid)) return;

        GameManager mgr = GameManager.getInstance();
        if (mgr == null) return;

        SoulDuo duo = mgr.getGameData().findDuoByPlayer(uuid);
        if (duo == null) return;

        UUID partnerUUID = duo.getPartner(uuid);
        if (partnerUUID == null) return;

        ServerPlayer partner = ((net.minecraft.server.level.ServerLevel) player.level())
                .getServer().getPlayerList().getPlayer(partnerUUID);
        if (partner == null) return;

        SYNCING.add(uuid);
        try {
            partner.setHealth(health);
        } finally {
            SYNCING.remove(uuid);
        }
    }
}
