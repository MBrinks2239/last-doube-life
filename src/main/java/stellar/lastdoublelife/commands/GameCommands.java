package stellar.lastdoublelife.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import stellar.lastdoublelife.config.ModConfig;
import stellar.lastdoublelife.manager.GameManager;

import static net.minecraft.commands.Commands.literal;

public class GameCommands {

    // /ldl start
    public static LiteralArgumentBuilder<CommandSourceStack> buildStart() {
        return literal("start").requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    GameManager mgr = GameManager.getInstance();
                    if (mgr == null) return DuoCommands.fail(ctx.getSource(), "Game not running");
                    if (mgr.getGameData().gameStarted)
                        return DuoCommands.fail(ctx.getSource(), "Game has already started");
                    if (mgr.getGameData().duos.isEmpty())
                        return DuoCommands.fail(ctx.getSource(), "No duos linked yet");
                    mgr.startGame();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "[LDL] Game started!").withStyle(ChatFormatting.GOLD), true);
                    return 1;
                });
    }

    // /ldl colors toggle
    public static LiteralArgumentBuilder<CommandSourceStack> buildColors() {
        return literal("colors").requires(s -> s.hasPermission(2))
                .then(literal("toggle")
                        .executes(ctx -> {
                            ModConfig cfg = ModConfig.get();
                            boolean nowOn = !cfg.showTabColors;
                            cfg.showTabColors = nowOn;
                            cfg.showLifeColorsIngame = nowOn;
                            cfg.save();
                            GameManager mgr = GameManager.getInstance();
                            if (mgr != null) {
                                if (nowOn) mgr.updateAllColors();
                                else for (var duo : mgr.getGameData().duos) mgr.removeFromAllTeams(duo);
                            }
                            boolean finalNowOn = nowOn;
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Life colors " + (finalNowOn ? "enabled" : "disabled") + ".").withStyle(ChatFormatting.YELLOW), true);
                            return 1;
                        }));
    }

    // /ldl config reload
    public static LiteralArgumentBuilder<CommandSourceStack> buildConfig() {
        return literal("config").requires(s -> s.hasPermission(2))
                .then(literal("reload")
                        .executes(ctx -> {
                            ModConfig.reload();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[LDL] Config reloaded.").withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }));
    }
}
