package stellar.lastdoublelife;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stellar.lastdoublelife.commands.RegisterCommands;
import stellar.lastdoublelife.common.services.ServerServices;


public class LastDoubleLife implements ModInitializer {
	public static final String MOD_ID = "last-double-life";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("[%s] Initializing Last Double Life Mod".formatted(MOD_ID));

		ServerLifecycleEvents.SERVER_STARTING.register(ServerServices::init);
		ServerLifecycleEvents.SERVER_STOPPED.register(ServerServices::clear);

		// Register commands
		RegisterCommands.registerAllCommands();


	}
}