package stellar.lastdoublelife;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stellar.lastdoublelife.commands.CommandRegistry;
import stellar.lastdoublelife.config.ModConfig;
import stellar.lastdoublelife.manager.GameManager;

public class LastDoubleLife implements ModInitializer {
	public static final String MOD_ID = "last-double-life";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[LDL] Initializing Last Double Life");

		ModConfig.get(); // load config on startup

		ServerLifecycleEvents.SERVER_STARTING.register(GameManager::init);
		ServerLifecycleEvents.SERVER_STOPPED.register(GameManager::shutdown);

		// Death: reduce lives + detect boogeyman kill in the same event
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			GameManager mgr = GameManager.getInstance();
			if (mgr == null) return;
			if (entity instanceof ServerPlayer player) {
				mgr.onPlayerDied(player, source);
			}
			if (source.getEntity() instanceof ServerPlayer killer) {
				mgr.onPlayerKilledEntity(killer, entity);
			}
		});

		// Respawn: put players with 0 lives into spectator
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			GameManager mgr = GameManager.getInstance();
			if (mgr != null) mgr.onPlayerRespawned(newPlayer);
		});

		// Tick: hunger/saturation sync + forbidden item removal (every 20 ticks)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			GameManager mgr = GameManager.getInstance();
			if (mgr != null && server.getTickCount() % 20 == 0) {
				mgr.onServerTick(server);
			}
		});

		CommandRegistry.register();

		LOGGER.info("[LDL] Last Double Life initialized");
	}
}
