package stellar.lastdoublelife.common.services;

import net.minecraft.server.MinecraftServer;
import stellar.lastdoublelife.LastDoubleLife;
import stellar.lastdoublelife.common.models.LifeDuos;
import stellar.lastdoublelife.common.storage.LifeDuosStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

public final class ServerServices {
    private static final Map<MinecraftServer, LifeDuosService> LIFE_DUOS = new WeakHashMap<>();

    private ServerServices() {}

    /** Call during server startup */
    public static void init(MinecraftServer server) {
        try {
            LifeDuosStorage storage = LifeDuosStorage.forWorld(server);
            LifeDuosService service = new LifeDuosService(storage);
            LIFE_DUOS.put(server, service);

            LastDoubleLife.LOGGER.info("LifeDuosService initialized. Path={}", storage.getFilePath());
        } catch (IOException e) {
            // If JSON is corrupted/unreadable, back it up and start fresh.
            try {
                LifeDuosStorage storage = LifeDuosStorage.forWorld(server);
                backupIfExists(storage.getFilePath());

                // Write a clean empty file, then load again.
                storage.save(new LifeDuos(new ArrayList<>()));

                LifeDuosService service = new LifeDuosService(storage);
                LIFE_DUOS.put(server, service);

                LastDoubleLife.LOGGER.error("Failed to load duos.json; created a fresh one. Reason={}", e.getMessage());
            } catch (Exception e2) {
                // At this point, something is seriously wrong with disk/path permissions.
                LastDoubleLife.LOGGER.error("Failed to initialize LifeDuosService: {}", e2.getMessage());
            }
        }
    }

    /** Call during server shutdown */
    public static void clear(MinecraftServer server) {
        LIFE_DUOS.remove(server);
    }

    /** Use from commands/events */
    public static LifeDuosService lifeDuos(MinecraftServer server) {
        LifeDuosService service = LIFE_DUOS.get(server);
        if (service == null) {
            throw new IllegalStateException("LifeDuosService not initialized yet (server lifecycle issue).");
        }
        return service;
    }

    private static void backupIfExists(Path file) throws IOException {
        if (!Files.exists(file)) return;

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = file.resolveSibling(file.getFileName().toString() + ".broken-" + ts);
        Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
    }
}
