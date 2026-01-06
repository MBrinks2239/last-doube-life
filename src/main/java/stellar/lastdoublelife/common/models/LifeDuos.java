package stellar.lastdoublelife.common.models;

import java.util.List;

public class LifeDuos {
    private final List<LifeDuo> duos;

    public LifeDuos(List<LifeDuo> duos) {
        this.duos = duos;
    }

    public List<LifeDuo> getDuos() {
        return duos;
    }

    public void addDuo(LifeDuo duo) throws IllegalArgumentException {
        if(containsPlayer(duo.player1()) || containsPlayer(duo.player2())) {
            throw new IllegalArgumentException("One of the players is already in a duo.");
        }
        duos.add(duo);
    }

    public void removeDuo(LifeDuo duo) {
        duos.remove(duo);
    }

    public boolean removeDuo(String player) {
        var duo = findDuoByPlayer(player);
        if (duo != null) {
            duos.remove(duo);
            return true;
        }
        return false;
    }

    public LifeDuo findDuoByPlayer(String player) {
        for (LifeDuo duo : duos) {
            if (duo.player1().equals(player) || duo.player2().equals(player)) {
                return duo;
            }
        }
        return null;
    }

    public boolean containsPlayer(String player) {
        return findDuoByPlayer(player) != null;
    }
}
