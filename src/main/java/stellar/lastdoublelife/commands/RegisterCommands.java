package stellar.lastdoublelife.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import stellar.lastdoublelife.suggestionsprovider.PlayerSuggestionProvider;

import static net.minecraft.commands.Commands.literal;

public class RegisterCommands {
    public static void registerAllCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("linkduo")
                    .then(Commands.argument("player_1", StringArgumentType.string()).suggests(new PlayerSuggestionProvider())
                            .then(Commands.argument("player_2", StringArgumentType.string()).suggests(new PlayerSuggestionProvider())
                                    .executes(LinkDuo::executeCommandWithArg))));
        });
    }
}
