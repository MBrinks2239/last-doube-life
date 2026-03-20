package stellar.lastdoublelife.data;

import java.util.UUID;

public class SoulDuo {
    public String player1; // UUID as string
    public String player2; // UUID as string
    public String duoName; // optional display name
    public int lives;
    public boolean isBoogeyman;

    // Transient: not persisted, cleared on restart
    public transient String pendingGiftTargetPlayer; // UUID string of target for pending life gift

    public SoulDuo() {}

    public SoulDuo(UUID p1, UUID p2, int lives) {
        this.player1 = p1.toString();
        this.player2 = p2.toString();
        this.lives = lives;
    }

    public UUID getPlayer1UUID() { return UUID.fromString(player1); }
    public UUID getPlayer2UUID() { return UUID.fromString(player2); }

    public boolean containsPlayer(UUID uuid) {
        String s = uuid.toString();
        return s.equals(player1) || s.equals(player2);
    }

    public UUID getPartner(UUID uuid) {
        String s = uuid.toString();
        if (s.equals(player1)) return UUID.fromString(player2);
        if (s.equals(player2)) return UUID.fromString(player1);
        return null;
    }

    public String getDisplayName() {
        return duoName != null ? duoName : (player1.substring(0, 6) + ".." + player2.substring(0, 6));
    }
}
