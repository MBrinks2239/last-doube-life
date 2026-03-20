package stellar.lastdoublelife.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import stellar.lastdoublelife.manager.GameManager;

import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class LivesCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("lives")

                // /ldl lives give <player> <amount>
                .then(literal("give").requires(s -> s.hasPermission(2))
                        .then(argument("player", StringArgumentType.word()).suggests(DuoCommands.ONLINE_PLAYERS)
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            UUID uuid = DuoCommands.resolveUUID(ctx.getSource(), name);
                                            if (uuid == null) return DuoCommands.fail(ctx.getSource(), "Player not found: " + name);
                                            GameManager mgr = GameManager.getInstance();
                                            if (mgr == null) return DuoCommands.fail(ctx.getSource(), "Game not running");
                                            String err = mgr.giveLives(uuid, amount);
                                            if (err != null) return DuoCommands.fail(ctx.getSource(), err);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Gave " + amount + " lives to " + name + "'s duo.").withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))

                // /ldl lives take <player> <amount>
                .then(literal("take").requires(s -> s.hasPermission(2))
                        .then(argument("player", StringArgumentType.word()).suggests(DuoCommands.ONLINE_PLAYERS)
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "player");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            UUID uuid = DuoCommands.resolveUUID(ctx.getSource(), name);
                                            if (uuid == null) return DuoCommands.fail(ctx.getSource(), "Player not found: " + name);
                                            GameManager mgr = GameManager.getInstance();
                                            if (mgr == null) return DuoCommands.fail(ctx.getSource(), "Game not running");
                                            String err = mgr.takeLives(uuid, amount);
                                            if (err != null) return DuoCommands.fail(ctx.getSource(), err);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Took " + amount + " lives from " + name + "'s duo.").withStyle(ChatFormatting.YELLOW), true);
                                            return 1;
                                        }))))

                // /ldl lives gift <player>  — initiates a life gift from your duo to target's duo
                .then(literal("gift")
                        .then(argument("player", StringArgumentType.word()).suggests(DuoCommands.ONLINE_PLAYERS)
                                .executes(ctx -> {
                                    ServerPlayer self;
                                    try { self = ctx.getSource().getPlayerOrException(); }
                                    catch (Exception e) { return DuoCommands.fail(ctx.getSource(), "Must be a player"); }
                                    String name = StringArgumentType.getString(ctx, "player");
                                    UUID targetUUID = DuoCommands.resolveUUID(ctx.getSource(), name);
                                    if (targetUUID == null) return DuoCommands.fail(ctx.getSource(), "Player not found: " + name);
                                    GameManager mgr = GameManager.getInstance();
                                    if (mgr == null) return DuoCommands.fail(ctx.getSource(), "Game not running");
                                    String err = mgr.initiateGiftLife(self.getUUID(), targetUUID);
                                    if (err != null) return DuoCommands.fail(ctx.getSource(), err);
                                    return 1;
                                }))

                        // /ldl lives gift confirm
                        .then(literal("confirm")
                                .executes(ctx -> {
                                    ServerPlayer self;
                                    try { self = ctx.getSource().getPlayerOrException(); }
                                    catch (Exception e) { return DuoCommands.fail(ctx.getSource(), "Must be a player"); }
                                    GameManager mgr = GameManager.getInstance();
                                    if (mgr == null) return DuoCommands.fail(ctx.getSource(), "Game not running");
                                    String err = mgr.confirmGiftLife(self.getUUID());
                                    if (err != null) return DuoCommands.fail(ctx.getSource(), err);
                                    return 1;
                                }))

                        // /ldl lives gift deny
                        .then(literal("deny")
                                .executes(ctx -> {
                                    ServerPlayer self;
                                    try { self = ctx.getSource().getPlayerOrException(); }
                                    catch (Exception e) { return DuoCommands.fail(ctx.getSource(), "Must be a player"); }
                                    GameManager mgr = GameManager.getInstance();
                                    if (mgr == null) return DuoCommands.fail(ctx.getSource(), "Game not running");
                                    String err = mgr.denyGiftLife(self.getUUID());
                                    if (err != null) return DuoCommands.fail(ctx.getSource(), err);
                                    self.sendSystemMessage(Component.literal("[LDL] Gift denied.").withStyle(ChatFormatting.RED));
                                    return 1;
                                })));
    }
}
