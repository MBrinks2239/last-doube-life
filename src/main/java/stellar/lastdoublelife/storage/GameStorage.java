package stellar.lastdoublelife.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import stellar.lastdoublelife.LastDoubleLife;
import stellar.lastdoublelife.data.GameData;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GameStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;

    public GameStorage(Path filePath) {
        this.filePath = filePath;
    }

    public static GameStorage forWorld(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("lastdoublelife");
        return new GameStorage(dir.resolve("gamedata.json"));
    }

    public GameData load() {
        if (!Files.exists(filePath)) return new GameData();
        try (Reader r = Files.newBufferedReader(filePath)) {
            GameData data = GSON.fromJson(r, GameData.class);
            return data != null ? data : new GameData();
        } catch (Exception e) {
            LastDoubleLife.LOGGER.error("[LDL] Corrupt game data, backing up and starting fresh: {}", e.getMessage());
            backup();
            return new GameData();
        }
    }

    public void save(GameData data) {
        try {
            Files.createDirectories(filePath.getParent());
            Path tmp = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(data, w);
            }
            try {
                Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LastDoubleLife.LOGGER.error("[LDL] Failed to save game data: {}", e.getMessage());
        }
    }

    private void backup() {
        try {
            if (!Files.exists(filePath)) return;
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Files.move(filePath, filePath.resolveSibling("gamedata.broken-" + ts + ".json"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LastDoubleLife.LOGGER.error("[LDL] Failed to backup corrupt data: {}", e.getMessage());
        }
    }
}
