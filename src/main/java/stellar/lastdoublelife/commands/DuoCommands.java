package stellar.lastdoublelife.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import stellar.lastdoublelife.data.GameData;
import stellar.lastdoublelife.data.SoulDuo;
import stellar.lastdoublelife.manager.GameManager;

import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DuoCommands {

    static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS =
            (ctx, builder) -> {
                ctx.getSource().getOnlinePlayerNames().forEach(builder::suggest);
                return builder.buildFuture();
            };

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("duo")
                // /ldl duo link <p1> <p2>
                .then(literal("link").requires(s -> s.hasPermission(2))
                        .then(argument("player1", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                .then(argument("player2", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                        .executes(ctx -> {
                                            String n1 = StringArgumentType.getString(ctx, "player1");
                                            String n2 = StringArgumentType.getString(ctx, "player2");
                                            UUID u1 = resolveUUID(ctx.getSource(), n1);
                                            UUID u2 = resolveUUID(ctx.getSource(), n2);
                                            if (u1 == null) return fail(ctx.getSource(), "Player not found: " + n1);
                                            if (u2 == null) return fail(ctx.getSource(), "Player not found: " + n2);
                                            GameManager mgr = GameManager.getInstance();
                                            if (mgr == null) return fail(ctx.getSource(), "Game not running");
                                            String err = mgr.linkDuo(u1, u2);
                                            if (err != null) return fail(ctx.getSource(), err);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Linked " + n1 + " <-> " + n2).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))

                // /ldl duo unlink <player>
                .then(literal("unlink").requires(s -> s.hasPermission(2))
                        .then(argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "player");
                                    UUID uuid = resolveUUID(ctx.getSource(), name);
                                    if (uuid == null) return fail(ctx.getSource(), "Player not found: " + name);
                                    GameManager mgr = GameManager.getInstance();
                                    if (mgr == null) return fail(ctx.getSource(), "Game not running");
                                    String err = mgr.unlinkDuo(uuid);
                                    if (err != null) return fail(ctx.getSource(), err);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Unlinked " + name).withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })))

                // /ldl duo name <player> <name>
                .then(literal("name").requires(s -> s.hasPermission(2))
                        .then(argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                .then(argument("duoname", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String player = StringArgumentType.getString(ctx, "player");
                                            String duoname = StringArgumentType.getString(ctx, "duoname");
                                            UUID uuid = resolveUUID(ctx.getSource(), player);
                                            if (uuid == null) return fail(ctx.getSource(), "Player not found: " + player);
                                            GameManager mgr = GameManager.getInstance();
                                            if (mgr == null) return fail(ctx.getSource(), "Game not running");
                                            String err = mgr.nameDuo(uuid, duoname);
                                            if (err != null) return fail(ctx.getSource(), err);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "Duo named: " + duoname).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))

                // /ldl duo list
                .then(literal("list").requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            GameManager mgr = GameManager.getInstance();
                            if (mgr == null) return fail(ctx.getSource(), "Game not running");
                            GameData data = mgr.getGameData();
                            if (data.duos.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No duos linked."), false);
                                return 1;
                            }
                            StringBuilder sb = new StringBuilder("Duos (" + data.duos.size() + "):\n");
                            for (SoulDuo duo : data.duos) {
                                String p1 = mgr.playerName(duo.getPlayer1UUID());
                                String p2 = mgr.playerName(duo.getPlayer2UUID());
                                String label = duo.duoName != null ? " [" + duo.duoName + "]" : "";
                                sb.append("  ").append(p1).append(" <-> ").append(p2).append(label);
                                if (data.gameStarted) sb.append(" | Lives: ").append(duo.lives);
                                if (duo.isBoogeyman) sb.append(" [BOOGEYMAN]");
                                sb.append("\n");
                            }
                            String msg = sb.toString().trim();
                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                            return 1;
                        }))

                // /ldl duo info [player]
                .then(literal("info")
                        .executes(ctx -> showInfo(ctx.getSource(), null))
                        .then(argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "player");
                                    UUID uuid = resolveUUID(ctx.getSource(), name);
                                    if (uuid == null) return fail(ctx.getSource(), "Player not found: " + name);
                                    return showInfoForUUID(ctx.getSource(), uuid);
                                })))

                // /ldl duo confirm <player>  — verify who your soulmate is
                .then(literal("confirm")
                        .then(argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                .executes(ctx -> {
                                    ServerPlayer self = ctx.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(ctx, "player");
                                    UUID targetUUID = resolveUUID(ctx.getSource(), targetName);
                                    if (targetUUID == null) return fail(ctx.getSource(), "Player not found: " + targetName);
                                    GameManager mgr = GameManager.getInstance();
                                    if (mgr == null) return fail(ctx.getSource(), "Game not running");
                                    SoulDuo duo = mgr.getGameData().findDuoByPlayer(self.getUUID());
                                    if (duo == null) return fail(ctx.getSource(), "You are not in a duo");
                                    UUID partner = duo.getPartner(self.getUUID());
                                    if (!targetUUID.equals(partner)) {
                                        self.sendSystemMessage(Component.literal(
                                                targetName + " is NOT your soulmate.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    String p1 = mgr.playerName(duo.getPlayer1UUID());
                                    String p2 = mgr.playerName(duo.getPlayer2UUID());
                                    String info = p1 + " <-> " + p2;
                                    if (duo.duoName != null) info += " [" + duo.duoName + "]";
                                    if (mgr.getGameData().gameStarted) info += " | Lives: " + duo.lives;
                                    final String finalInfo = info;
                                    self.sendSystemMessage(Component.literal(
                                            "Confirmed! " + finalInfo).withStyle(ChatFormatting.GREEN));
                                    return 1;
                                })));
    }

    private static int showInfo(CommandSourceStack source, UUID override) {
        try {
            ServerPlayer self = source.getPlayerOrException();
            UUID uuid = override != null ? override : self.getUUID();
            return showInfoForUUID(source, uuid);
        } catch (Exception e) {
            return fail(source, "Must be a player to use this command without arguments");
        }
    }

    private static int showInfoForUUID(CommandSourceStack source, UUID uuid) {
        GameManager mgr = GameManager.getInstance();
        if (mgr == null) return fail(source, "Game not running");
        SoulDuo duo = mgr.getGameData().findDuoByPlayer(uuid);
        if (duo == null) {
            source.sendSuccess(() -> Component.literal(mgr.playerName(uuid) + " is not in a duo."), false);
            return 0;
        }
        String p1 = mgr.playerName(duo.getPlayer1UUID());
        String p2 = mgr.playerName(duo.getPlayer2UUID());
        String label = duo.duoName != null ? " \"" + duo.duoName + "\"" : "";
        String lives = mgr.getGameData().gameStarted ? " | Lives: " + duo.lives : "";
        String boogeyman = duo.isBoogeyman ? " [BOOGEYMAN]" : "";
        source.sendSuccess(() -> Component.literal(
                p1 + " <-> " + p2 + label + lives + boogeyman), false);
        return 1;
    }

    static UUID resolveUUID(CommandSourceStack source, String name) {
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(name)) return p.getUUID();
        }
        return null;
    }

    static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
