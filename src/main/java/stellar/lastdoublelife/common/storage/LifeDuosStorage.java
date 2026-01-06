package stellar.lastdoublelife.common.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import stellar.lastdoublelife.common.models.LifeDuo;
import stellar.lastdoublelife.common.models.LifeDuos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores LifeDuos as JSON in the world save folder.
 *
 * File format:
 * [
 *   {"player1":"Alice","player2":"Bob"},
 *   {"player1":"Carol","player2":"Dave"}
 * ]
 */
public final class LifeDuosStorage {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Type DUO_LIST_TYPE = new TypeToken<List<LifeDuoData>>() {}.getType();

    private final Path filePath;

    public LifeDuosStorage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Per-world storage under the world save root.
     * Example: <world>/lastdoublelife/duos.json
     */
    public static LifeDuosStorage forWorld(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path dir = worldRoot.resolve("lastdoublelife");
        return new LifeDuosStorage(dir.resolve("duos.json"));
    }

    public Path getFilePath() {
        return filePath;
    }

    public LifeDuos load() throws IOException {
        if (!Files.exists(filePath)) {
            return new LifeDuos(new ArrayList<>());
        }

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            List<LifeDuoData> data = GSON.fromJson(reader, DUO_LIST_TYPE);
            if (data == null) data = new ArrayList<>();

            List<LifeDuo> duos = new ArrayList<>(data.size());
            for (LifeDuoData d : data) {
                if (d == null || isBlank(d.player1) || isBlank(d.player2)) continue;
                duos.add(new LifeDuo(d.player1, d.player2));
            }

            return new LifeDuos(duos);
        }
    }

    /**
     * Saves atomically (write temp -> move/replace) to reduce chance of corrupt JSON on crash.
     */
    public void save(LifeDuos lifeDuos) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null) Files.createDirectories(parent);

        List<LifeDuoData> out = new ArrayList<>();
        for (LifeDuo duo : lifeDuos.getDuos()) {
            out.add(new LifeDuoData(duo.player1(), duo.player2()));
        }

        Path tmp = filePath.resolveSibling(filePath.getFileName().toString() + ".tmp");

        try (Writer writer = Files.newBufferedWriter(
                tmp,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            GSON.toJson(out, DUO_LIST_TYPE, writer);
        }

        try {
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // DTO for Gson
    private static final class LifeDuoData {
        String player1;
        String player2;

        @SuppressWarnings("unused")
        LifeDuoData() {}

        LifeDuoData(String player1, String player2) {
            this.player1 = player1;
            this.player2 = player2;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
