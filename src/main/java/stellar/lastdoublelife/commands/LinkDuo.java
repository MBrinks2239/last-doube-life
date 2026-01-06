package stellar.lastdoublelife.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import stellar.lastdoublelife.common.models.LifeDuo;
import stellar.lastdoublelife.common.models.LifeDuos;
import stellar.lastdoublelife.common.services.LifeDuosService;
import stellar.lastdoublelife.common.services.ServerServices;

public class LinkDuo {
    private static final String PLAYER_1_ARG = "player_1";
    private static final String PLAYER_2_ARG = "player_2";

    public static int executeCommandWithArg(CommandContext<CommandSourceStack> context) {
        var player1 = StringArgumentType.getString(context, PLAYER_1_ARG);
        var player2 = StringArgumentType.getString(context, PLAYER_2_ARG);
        try {
            var server = context.getSource().getServer();
            LifeDuosService service = ServerServices.lifeDuos(server);

            service.addDuo(new LifeDuo(player1, player2));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("- %s <-> %s".formatted(player1, player2)), false);
        return 1;
    }
}
