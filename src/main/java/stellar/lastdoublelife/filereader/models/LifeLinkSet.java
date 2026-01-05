package stellar.lastdoublelife.filereader.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LifeLinkSet {
    private final List<LifeDuo> duos = new ArrayList<>();
    private final List<LifeDuo> preCommitDuo = new ArrayList<>();

    // Instance storage path so callers can provide the world save folder. The file will be placed under
    // <worldSaveDir>/lastdoublelife/duos.json. If null/defaulted, we fall back to ./world/lastdoublelife/duos.json.
    private final Path storage;

    // Default constructor: use workspace/local "world" folder. Prefer using the other constructor that
    // accepts the real world save folder from the server runtime.
    public LifeLinkSet() {
        this(Paths.get("world"));
    }

    // Construct and store under the given world save folder. The file will live at
    // worldSaveFolder/lastdoublelife/duos.json
    public LifeLinkSet(Path worldSaveFolder) {
        if (worldSaveFolder == null) {
            worldSaveFolder = Paths.get("world");
        }
        this.storage = worldSaveFolder.resolve("lastdoublelife").resolve("duos.json");
        loadFromFile();
    }

    public List<LifeDuo> getDuos() {
        return Collections.unmodifiableList(duos);
    }

    private List<String> getAllPlayers() {
        List<String> players = new ArrayList<>();
        for (LifeDuo duo : duos) {
            players.add(duo.player1UUID);
            players.add(duo.player2UUID);
        }
        return players;
    }

    public boolean addDuos(LifeDuo... duos) {
        for (LifeDuo duo : duos) {
            if (getAllPlayers().contains(duo.player1UUID) || getAllPlayers().contains(duo.player2UUID)) {
                // One of the players is already linked
                return false;
            }
        }
        synchronized (this) {
            this.preCommitDuo.addAll(Arrays.stream(duos).toList());
            writeDuosToFile();
        }
        return true;
    }

    public void clearDuos() {
        synchronized (this) {
            this.preCommitDuo.clear();
            writeDuosToFile();
        }
    }

    public boolean writeDuosToFile() {
        synchronized (this) {
            try {
                // Ensure parent directory exists
                Path parent = storage.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(preCommitDuo);

                // Write to a temp file first, then move into place for atomicity when possible
                Path tmp = storage.resolveSibling(storage.getFileName().toString() + ".tmp");
                Files.writeString(tmp, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                try {
                    Files.move(tmp, storage, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException amnse) {
                    // fallback to non-atomic move
                    Files.move(tmp, storage, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                // Minimal logging so server doesn't crash; consider replacing with proper logger
                System.err.println("Failed writing duos to " + storage + ": " + e.getMessage());
                e.printStackTrace(System.err);
                return false;
            }

            this.duos.clear();
            this.duos.addAll(preCommitDuo);
            return true;
        }
    }

    private void loadFromFile() {
        synchronized (this) {
            try {
                if (!Files.exists(storage)) {
                    // nothing to load
                    return;
                }

                String json = Files.readString(storage);
                if (json.isBlank()) {
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                List<LifeDuo> read = mapper.readValue(json, new TypeReference<>() {});
                this.preCommitDuo.clear();
                if (read != null && !read.isEmpty()) {
                    this.preCommitDuo.addAll(read);
                    this.duos.clear();
                    this.duos.addAll(read);
                }
            } catch (Exception e) {
                // If loading fails, print stack and continue with empty lists so server can still run
                System.err.println("Failed loading duos from " + storage + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }
}
