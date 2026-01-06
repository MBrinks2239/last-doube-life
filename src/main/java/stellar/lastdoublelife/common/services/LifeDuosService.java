package stellar.lastdoublelife.common.services;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import stellar.lastdoublelife.LastDoubleLife;
import stellar.lastdoublelife.common.models.LifeDuo;
import stellar.lastdoublelife.common.models.LifeDuos;
import stellar.lastdoublelife.common.storage.LifeDuosStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class LifeDuosService {

    private final LifeDuosStorage storage;
    private final LifeDuos duos;

    public LifeDuosService(LifeDuosStorage storage) throws IOException {
        this.storage = storage;
        this.duos = storage.load();
    }

    public List<LifeDuo> getAll() { return duos.getDuos(); }

    public void addDuo(LifeDuo duo) {
        try {
            duos.addDuo(duo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot add duo: " + e.getMessage());
        }

        try {
            storage.save(duos);
        } catch (Exception e) {
            duos.removeDuo(duo); // rollback in-memory state on failure
            LastDoubleLife.LOGGER.error("Failed to save duos after adding new duo: {}", e.getMessage());
        }
    }

    public boolean removeDuo(String player) {
        LifeDuo duo = duos.findDuoByPlayer(player);
        boolean removed = duos.removeDuo(player);
        try {
            if (removed) storage.save(duos);
        } catch (Exception e) {
            // rollback in-memory state on failure (since exception only occurs during save)
            if (duo != null) {
                duos.addDuo(duo);
            }
            LastDoubleLife.LOGGER.error("Failed to save duos after removing duo: {}", e.getMessage());
        }
        return removed;
    }

    public void removeDuo(LifeDuo duo) {
        duos.removeDuo(duo);

        try {
            storage.save(duos);
        } catch (Exception e) {
            // rollback in-memory state on failure (since exception only occurs during save)
            duos.addDuo(duo);
            LastDoubleLife.LOGGER.error("Failed to save duos after removing duo: {}", e.getMessage());
        }
    }
}

