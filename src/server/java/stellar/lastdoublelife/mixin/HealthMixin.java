package stellar.lastdoublelife.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.WorldSavePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import stellar.lastdoublelife.filereader.models.LifeDuo;
import stellar.lastdoublelife.filereader.models.LifeLinkSet;

import java.nio.file.Path;
import java.util.*;

@Mixin(LivingEntity.class)
public abstract class HealthMixin {

    // Track UUID strings currently being propagated to avoid re-entrance / infinite loops
    private static final Set<String> PROPAGATING = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Path, LifeLinkSet> CACHE = new HashMap<>();

    @Inject(method = "heal", at = @At("HEAD"))
    private void onHeal(float amount, CallbackInfo ci) {
        Object thiz = (Object) this;
        if (!(thiz instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) thiz;

        String uuid = player.getUuidAsString();
        if (PROPAGATING.contains(uuid)) return;

        try {
            LifeLinkSet set = getLifeLinkSetForPlayer(player);
            for (LifeDuo duo : set.getDuos()) {
                String otherUuid = null;
                if (uuid.equals(duo.player1UUID)) otherUuid = duo.player2UUID;
                else if (uuid.equals(duo.player2UUID)) otherUuid = duo.player1UUID;

                if (otherUuid == null) continue;

                UUID otherId;
                try {
                    otherId = UUID.fromString(otherUuid);
                } catch (Exception e) {
                    continue;
                }

                ServerPlayerEntity other = player.getServer().getPlayerManager().getPlayer(otherId);
                if (other != null) {
                    // mark to avoid recursion
                    PROPAGATING.add(otherUuid);
                    other.heal(amount);
                    PROPAGATING.remove(otherUuid);
                }
            }
        } catch (Exception e) {
            System.err.println("Error propagating heal for " + uuid + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Object thiz = (Object) this;
        if (!(thiz instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) thiz;

        String uuid = player.getUuidAsString();
        if (PROPAGATING.contains(uuid)) return;

        try {
            LifeLinkSet set = getLifeLinkSetForPlayer(player);
            for (LifeDuo duo : set.getDuos()) {
                String otherUuid = null;
                if (uuid.equals(duo.player1UUID)) otherUuid = duo.player2UUID;
                else if (uuid.equals(duo.player2UUID)) otherUuid = duo.player1UUID;

                if (otherUuid == null) continue;

                UUID otherId;
                try {
                    otherId = UUID.fromString(otherUuid);
                } catch (Exception e) {
                    continue;
                }

                ServerPlayerEntity other = player.getServer().getPlayerManager().getPlayer(otherId);
                if (other != null) {
                    // mark to avoid recursion
                    PROPAGATING.add(otherUuid);
                    other.damage(source, amount);
                    PROPAGATING.remove(otherUuid);
                }
            }
        } catch (Exception e) {
            System.err.println("Error propagating damage for " + uuid + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static LifeLinkSet getLifeLinkSetForPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        Path worldPath = server.getSavePath(WorldSavePath.ROOT);
        Path key = worldPath.toAbsolutePath().normalize();
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(key, k -> new LifeLinkSet(k));
        }
    }
}

