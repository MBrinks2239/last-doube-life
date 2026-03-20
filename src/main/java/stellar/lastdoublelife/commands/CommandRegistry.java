package stellar.lastdoublelife.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import static net.minecraft.commands.Commands.literal;

public class CommandRegistry {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("ldl")
                    .then(DuoCommands.build())
                    .then(LivesCommands.build())
                    .then(BoogeymanCommands.build())
                    .then(GameCommands.buildStart())
                    .then(GameCommands.buildColors())
                    .then(GameCommands.buildConfig())
            );
        });
    }
}
