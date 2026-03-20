package stellar.lastdoublelife.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import stellar.lastdoublelife.manager.GameManager;

import static net.minecraft.commands.Commands.literal;

public class BoogeymanCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("boogeyman")

                // /ldl boogeyman roll
                .then(literal("roll").requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            GameManager mgr = GameManager.getInstance();
                            if (mgr == null) return DuoCommands.fail(ctx.getSource(), "Game not running");
                            String err = mgr.rollBoogeyman();
                            if (err != null) return DuoCommands.fail(ctx.getSource(), err);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[LDL] Boogeyman rolled! They have been notified privately.").withStyle(ChatFormatting.DARK_RED), true);
                            return 1;
                        }));
    }
}
