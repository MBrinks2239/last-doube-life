package stellar.lastdoublelife.manager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import stellar.lastdoublelife.LastDoubleLife;
import stellar.lastdoublelife.config.ModConfig;
import stellar.lastdoublelife.data.GameData;
import stellar.lastdoublelife.data.SoulDuo;
import stellar.lastdoublelife.storage.GameStorage;

import java.util.*;

public class GameManager {

    public static final String TEAM_GREEN  = "ldl_green";
    public static final String TEAM_YELLOW = "ldl_yellow";
    public static final String TEAM_RED    = "ldl_red";
    public static final String TEAM_DEAD   = "ldl_dead";

    private static GameManager instance;

    private final MinecraftServer server;
    private final GameStorage storage;
    private GameData data;

    // partner UUID -> target player UUID (waiting for partner to confirm gift)
    private final Map<UUID, UUID> pendingGiftConfirmations = new HashMap<>();

    // Previous food/saturation levels — used to detect who ate so we propagate the increase
    private final Map<UUID, Integer> prevFood = new HashMap<>();
    private final Map<UUID, Float>   prevSat  = new HashMap<>();

    private GameManager(MinecraftServer server) {
        this.server = server;
        this.storage = GameStorage.forWorld(server);
        this.data = storage.load();
    }

    public static void init(MinecraftServer server) {
        instance = new GameManager(server);
        LastDoubleLife.LOGGER.info("[LDL] GameManager initialized");
    }

    public static void shutdown(MinecraftServer server) {
        if (instance != null) {
            instance.storage.save(instance.data);
            instance = null;
        }
    }

    public static GameManager getInstance() { return instance; }

    public GameData getGameData() { return data; }

    // ---- Duo Linking ----

    /** Returns null on success, error string on failure. */
    public String linkDuo(UUID p1, UUID p2) {
        if (p1.equals(p2)) return "Cannot link a player to themselves";
        if (data.isPlayerLinked(p1)) return playerName(p1) + " is already in a duo";
        if (data.isPlayerLinked(p2)) return playerName(p2) + " is already in a duo";
        data.duos.add(new SoulDuo(p1, p2, 0));
        save();
        return null;
    }

    public String unlinkDuo(UUID player) {
        if (!data.isPlayerLinked(player)) return playerName(player) + " is not in a duo";
        data.removeDuoByPlayer(player);
        save();
        return null;
    }

    public String nameDuo(UUID player, String name) {
        SoulDuo duo = data.findDuoByPlayer(player);
        if (duo == null) return playerName(player) + " is not in a duo";
        duo.duoName = name == null || name.isBlank() ? null : name;
        save();
        return null;
    }

    // ---- Game Start ----

    public void startGame() {
        Random rng = new Random();
        ModConfig cfg = ModConfig.get();
        int range = Math.max(0, cfg.maxLives - cfg.minLives);
        for (SoulDuo duo : data.duos) {
            duo.lives = cfg.minLives + (range > 0 ? rng.nextInt(range + 1) : 0);
        }
        data.gameStarted = true;
        save();
        setupTeams();
        updateAllColors();
        broadcastAll(Component.literal("[LDL] The game has started! Good luck!").withStyle(ChatFormatting.GOLD));
    }

    // ---- Death / Kill ----

    public void onPlayerDied(ServerPlayer player, DamageSource source) {
        if (!data.gameStarted) return;
        SoulDuo duo = data.findDuoByPlayer(player.getUUID());
        if (duo == null) return;

        duo.lives = Math.max(0, duo.lives - 1);
        save();
        updateColor(duo);

        String remaining = duo.lives == 1 ? "1 life" : duo.lives + " lives";
        sendToDuo(duo, Component.literal("A life was lost! " + remaining + " remaining.")
                .withStyle(ChatFormatting.RED));

        if (duo.lives <= 0) {
            sendToDuo(duo, Component.literal("You have no lives left. You are permanently eliminated!")
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    public void onPlayerRespawned(ServerPlayer player) {
        if (!data.gameStarted) return;
        SoulDuo duo = data.findDuoByPlayer(player.getUUID());
        if (duo != null && duo.lives <= 0) {
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    public void onPlayerKilledEntity(ServerPlayer killer, LivingEntity victim) {
        if (!data.gameStarted) return;
        if (!(victim instanceof ServerPlayer)) return;

        SoulDuo duo = data.findDuoByPlayer(killer.getUUID());
        if (duo == null || !duo.isBoogeyman) return;

        duo.isBoogeyman = false;
        save();
        updateColor(duo);
        sendToDuo(duo, Component.literal("[BOOGEYMAN] You are cured!").withStyle(ChatFormatting.GREEN));
    }

    // ---- Boogeyman ----

    public String rollBoogeyman() {
        List<SoulDuo> eligible = data.duos.stream()
                .filter(d -> d.lives > 0 && !d.isBoogeyman)
                .toList();
        if (eligible.isEmpty()) return "No eligible duos to assign as Boogeyman";

        SoulDuo chosen = eligible.get(new Random().nextInt(eligible.size()));
        chosen.isBoogeyman = true;
        save();
        updateColor(chosen);

        sendToDuo(chosen, Component.literal(
                "[BOOGEYMAN] You have been chosen! You must kill someone to be cured. Keep it secret!")
                .withStyle(ChatFormatting.DARK_RED));
        return null;
    }

    public String removeBoogeyman(UUID player) {
        SoulDuo duo = data.findDuoByPlayer(player);
        if (duo == null) return playerName(player) + " is not in a duo";
        if (!duo.isBoogeyman) return playerName(player) + " is not the Boogeyman";

        duo.isBoogeyman = false;
        save();
        updateColor(duo);
        sendToDuo(duo, Component.literal("[BOOGEYMAN] You are no longer the Boogeyman.")
                .withStyle(ChatFormatting.GREEN));
        return null;
    }

    // ---- Admin Lives ----

    public String giveLives(UUID player, int amount) {
        SoulDuo duo = data.findDuoByPlayer(player);
        if (duo == null) return playerName(player) + " is not in a duo";
        duo.lives += amount;
        save();
        updateColor(duo);
        return null;
    }

    public String takeLives(UUID player, int amount) {
        SoulDuo duo = data.findDuoByPlayer(player);
        if (duo == null) return playerName(player) + " is not in a duo";
        duo.lives = Math.max(0, duo.lives - amount);
        save();
        updateColor(duo);
        return null;
    }

    // ---- Gift Life ----

    /**
     * Player 'from' initiates gifting 1 life to 'to's duo.
     * 'from's soulmate must confirm with /ldl lives gift confirm.
     */
    public String initiateGiftLife(UUID from, UUID to) {
        SoulDuo giverDuo = data.findDuoByPlayer(from);
        if (giverDuo == null) return "You are not in a duo";
        if (giverDuo.lives <= 1) return "Your duo needs at least 2 lives to gift one away";

        SoulDuo receiverDuo = data.findDuoByPlayer(to);
        if (receiverDuo == null) return playerName(to) + " is not in a duo";
        if (giverDuo == receiverDuo) return "Cannot gift lives within your own duo";

        UUID partner = giverDuo.getPartner(from);
        if (partner == null) return "Could not determine your partner";

        giverDuo.pendingGiftTargetPlayer = to.toString();
        pendingGiftConfirmations.put(partner, to);

        ServerPlayer partnerPlayer = server.getPlayerList().getPlayer(partner);
        if (partnerPlayer != null) {
            partnerPlayer.sendSystemMessage(Component.literal(
                    "[LDL] Your soulmate wants to gift a life to " + playerName(to) +
                    "'s duo. Run /ldl lives gift confirm to approve, or /ldl lives gift deny to cancel."
            ).withStyle(ChatFormatting.YELLOW));
        }

        ServerPlayer fromPlayer = server.getPlayerList().getPlayer(from);
        if (fromPlayer != null) {
            fromPlayer.sendSystemMessage(Component.literal(
                    "[LDL] Gift request sent. Waiting for your soulmate to confirm."
            ).withStyle(ChatFormatting.YELLOW));
        }
        return null;
    }

    public String confirmGiftLife(UUID confirmer) {
        UUID target = pendingGiftConfirmations.get(confirmer);
        if (target == null) return "You have no pending gift life to confirm";

        SoulDuo giverDuo = data.findDuoByPlayer(confirmer);
        if (giverDuo == null) {
            pendingGiftConfirmations.remove(confirmer);
            return "You are not in a duo";
        }
        if (giverDuo.lives <= 1) {
            pendingGiftConfirmations.remove(confirmer);
            return "Your duo no longer has enough lives to gift";
        }

        SoulDuo receiverDuo = data.findDuoByPlayer(target);
        if (receiverDuo == null) {
            pendingGiftConfirmations.remove(confirmer);
            return "The target player is no longer in a duo";
        }

        giverDuo.lives--;
        giverDuo.pendingGiftTargetPlayer = null;
        receiverDuo.lives++;
        pendingGiftConfirmations.remove(confirmer);
        save();
        updateColor(giverDuo);
        updateColor(receiverDuo);

        sendToDuo(giverDuo, Component.literal("[LDL] Gifted 1 life. You now have " + giverDuo.lives + " lives.")
                .withStyle(ChatFormatting.YELLOW));
        sendToDuo(receiverDuo, Component.literal("[LDL] Received 1 life! You now have " + receiverDuo.lives + " lives.")
                .withStyle(ChatFormatting.GREEN));
        return null;
    }

    public String denyGiftLife(UUID confirmer) {
        UUID target = pendingGiftConfirmations.remove(confirmer);
        if (target == null) return "No pending gift to deny";

        SoulDuo giverDuo = data.findDuoByPlayer(confirmer);
        if (giverDuo != null) giverDuo.pendingGiftTargetPlayer = null;

        sendToDuo(giverDuo != null ? giverDuo : null, null); // noop — notify initiator below
        // Notify the initiator (the other member of giverDuo)
        if (giverDuo != null) {
            UUID initiator = giverDuo.getPartner(confirmer);
            if (initiator != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(initiator);
                if (p != null) p.sendSystemMessage(
                        Component.literal("[LDL] Your soulmate denied the life gift.").withStyle(ChatFormatting.RED));
            }
        }
        return null;
    }

    // ---- Tick (hunger/saturation sync + forbidden items) ----

    public void onServerTick(MinecraftServer srv) {
        ModConfig cfg = ModConfig.get();

        if (cfg.syncHunger || cfg.syncSaturation) {
            for (SoulDuo duo : data.duos) {
                if (!data.gameStarted) continue;
                ServerPlayer p1 = srv.getPlayerList().getPlayer(UUID.fromString(duo.player1));
                ServerPlayer p2 = srv.getPlayerList().getPlayer(UUID.fromString(duo.player2));
                if (p1 == null || p2 == null) continue;

                FoodData f1 = p1.getFoodData();
                FoodData f2 = p2.getFoodData();
                UUID id1 = p1.getUUID();
                UUID id2 = p2.getUUID();

                if (cfg.syncHunger) {
                    int cur1 = f1.getFoodLevel();
                    int cur2 = f2.getFoodLevel();
                    int prev1 = prevFood.getOrDefault(id1, cur1);
                    int prev2 = prevFood.getOrDefault(id2, cur2);

                    if (cur1 > prev1) {
                        // p1 ate — boost p2 to the same level
                        f2.setFoodLevel(cur1);
                    } else if (cur2 > prev2) {
                        // p2 ate — boost p1 to the same level
                        f1.setFoodLevel(cur2);
                    } else if (cur1 != cur2) {
                        // Neither ate but levels diverged — sync to lower (shared starvation)
                        int min = Math.min(cur1, cur2);
                        f1.setFoodLevel(min);
                        f2.setFoodLevel(min);
                    }

                    prevFood.put(id1, f1.getFoodLevel());
                    prevFood.put(id2, f2.getFoodLevel());
                }
                if (cfg.syncSaturation) {
                    float cur1 = f1.getSaturationLevel();
                    float cur2 = f2.getSaturationLevel();
                    float prev1 = prevSat.getOrDefault(id1, cur1);
                    float prev2 = prevSat.getOrDefault(id2, cur2);

                    if (cur1 > prev1) {
                        f2.setSaturation(cur1);
                    } else if (cur2 > prev2) {
                        f1.setSaturation(cur2);
                    } else if (cur1 != cur2) {
                        float min = Math.min(cur1, cur2);
                        f1.setSaturation(min);
                        f2.setSaturation(min);
                    }

                    prevSat.put(id1, f1.getSaturationLevel());
                    prevSat.put(id2, f2.getSaturationLevel());
                }
            }
        }

        if (!cfg.forbiddenItems.isEmpty()) {
            for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    var stack = player.getInventory().getItem(i);
                    if (stack.isEmpty()) continue;
                    String id = stack.getItem().toString();
                    if (cfg.forbiddenItems.contains(id)) {
                        player.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                        player.sendSystemMessage(Component.literal(
                                "[LDL] " + id + " is a forbidden item and was removed from your inventory.")
                                .withStyle(ChatFormatting.RED));
                    }
                }
            }
        }
    }

    // ---- Display Colors via Teams ----

    public void setupTeams() {
        Scoreboard sb = server.getScoreboard();
        ensureTeam(sb, TEAM_GREEN,  ChatFormatting.GREEN);
        ensureTeam(sb, TEAM_YELLOW, ChatFormatting.YELLOW);
        ensureTeam(sb, TEAM_RED,    ChatFormatting.RED);
        ensureTeam(sb, TEAM_DEAD,   ChatFormatting.DARK_GRAY);
    }

    private void ensureTeam(Scoreboard sb, String name, ChatFormatting color) {
        PlayerTeam team = sb.getPlayerTeam(name);
        if (team == null) team = sb.addPlayerTeam(name);
        team.setColor(color);
        team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.ALWAYS);
    }

    public void updateAllColors() {
        for (SoulDuo duo : data.duos) updateColor(duo);
    }

    public void updateColor(SoulDuo duo) {
        if (duo == null) return;
        ModConfig cfg = ModConfig.get();
        if (!cfg.showTabColors && !cfg.showLifeColorsIngame) {
            removeFromAllTeams(duo);
            return;
        }
        String teamName = teamForLives(duo.lives, duo.isBoogeyman);
        assignToTeam(duo.player1, teamName);
        assignToTeam(duo.player2, teamName);
    }

    public void removeFromAllTeams(SoulDuo duo) {
        removePlayerFromAllTeams(duo.player1);
        removePlayerFromAllTeams(duo.player2);
    }

    private void removePlayerFromAllTeams(String playerUUID) {
        ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(playerUUID));
        if (p == null) return;
        removeFromLdlTeam(server.getScoreboard(), p.getName().getString());
    }

    /** Removes the player from whichever LDL team they are currently in (if any). */
    private void removeFromLdlTeam(Scoreboard sb, String playerName) {
        PlayerTeam current = sb.getPlayersTeam(playerName);
        if (current == null) return;
        String n = current.getName();
        if (n.equals(TEAM_GREEN) || n.equals(TEAM_YELLOW) || n.equals(TEAM_RED) || n.equals(TEAM_DEAD)) {
            sb.removePlayerFromTeam(playerName, current);
        }
    }

    private void assignToTeam(String playerUUID, String teamName) {
        if (playerUUID == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(UUID.fromString(playerUUID));
        if (p == null) return;
        String name = p.getName().getString();
        Scoreboard sb = server.getScoreboard();

        removeFromLdlTeam(sb, name);

        PlayerTeam target = sb.getPlayerTeam(teamName);
        if (target == null) {
            setupTeams();
            target = sb.getPlayerTeam(teamName);
        }
        if (target != null) sb.addPlayerToTeam(name, target);
    }

    private String teamForLives(int lives, boolean isBoogeyman) {
        if (lives <= 0) return TEAM_DEAD;
        // if (isBoogeyman) return TEAM_RED;
        if (lives <= 2) return TEAM_RED;
        if (lives <= 4) return TEAM_YELLOW;
        return TEAM_GREEN;
    }

    // ---- Helpers ----

    private void save() { storage.save(data); }

    public String playerName(UUID uuid) {
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        return p != null ? p.getName().getString() : uuid.toString().substring(0, 8) + "..";
    }

    private void sendToDuo(SoulDuo duo, Component msg) {
        if (duo == null || msg == null) return;
        sendToUUID(UUID.fromString(duo.player1), msg);
        sendToUUID(UUID.fromString(duo.player2), msg);
    }

    public void sendToUUID(UUID uuid, Component msg) {
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        if (p != null) p.sendSystemMessage(msg);
    }

    private void broadcastAll(Component msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
        }
    }
}
