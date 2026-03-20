package stellar.lastdoublelife.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameData {
    public boolean gameStarted = false;
    public List<SoulDuo> duos = new ArrayList<>();
    public List<String> soloPlayers = new ArrayList<>(); // UUIDs excluded from pairing

    public SoulDuo findDuoByPlayer(UUID uuid) {
        String s = uuid.toString();
        for (SoulDuo duo : duos) {
            if (s.equals(duo.player1) || s.equals(duo.player2)) return duo;
        }
        return null;
    }

    public boolean isPlayerLinked(UUID uuid) {
        return findDuoByPlayer(uuid) != null;
    }

    public void removeDuoByPlayer(UUID uuid) {
        String s = uuid.toString();
        duos.removeIf(d -> s.equals(d.player1) || s.equals(d.player2));
    }
}
