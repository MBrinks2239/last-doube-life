package stellar.lastdoublelife;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import stellar.lastdoublelife.filereader.models.LifeDuo;
import stellar.lastdoublelife.filereader.models.LifeLinkSet;
import stellar.lastdoublelife.suggestionsprovider.PlayerSuggestionProvider;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.literal;


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

		LOGGER.info("Hello Fabric world!");

		LifeLinkSet linkSet = new LifeLinkSet();

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("linkduo")
					.then(Commands.argument("player_1", StringArgumentType.string()).suggests(new PlayerSuggestionProvider())
					.then(Commands.argument("player_2", StringArgumentType.string()).suggests(new PlayerSuggestionProvider())
					.executes(context -> {
						String player1 = StringArgumentType.getString(context, "player_1");
						String player2 = StringArgumentType.getString(context, "player_2");

						LifeDuo duo = new LifeDuo();
						duo.player1UUID = player1;
						duo.player2UUID = player2;

						var result = linkSet.addDuos(duo);

						if (!result) {
							context.getSource().sendFailure(Component.literal("Failed to link duo: one or both players are already linked."));
							return 0; // Command failed
						}

						context.getSource().sendSuccess(() -> Component.literal("Linked duo: %s <-> %s".formatted(player1, player2)), false);

						return 1; // Command successful
			}))));

			dispatcher.register(literal("getduos")
					.executes(context -> {
						StringBuilder duosList = new StringBuilder("Current linked duos:");
						for (LifeDuo duo : linkSet.getDuos()) {
							duosList.append("\n- %s <-> %s".formatted(duo.player1UUID, duo.player2UUID));
						}
						context.getSource().sendSuccess(() -> Component.literal(duosList.toString()), false);

						return 1; // Command successful
			}));
		});
	}
}