package stellar.lastdoublelife.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import stellar.lastdoublelife.LastDoubleLife;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static ModConfig INSTANCE;
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("last-double-life.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Lives
    public int minLives = 3;
    public int maxLives = 6;

    // Syncing
    public boolean syncHealth = true;
    public boolean syncHunger = false;
    public boolean syncSaturation = false;

    // Display
    public boolean showLifeColorsIngame = true;
    public boolean showTabColors = true;

    // Restrictions
    public List<String> forbiddenItems = new ArrayList<>();
    public int maxPotionAmplifier = -1;  // -1 = no limit
    public int maxEnchantmentLevel = -1; // -1 = no limit

    // Pre-linked duos applied at game start: [["player1Name", "player2Name"], ...]
    public List<List<String>> prelinkedDuos = new ArrayList<>();

    public static ModConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = load();
        LastDoubleLife.LOGGER.info("[LDL] Config reloaded");
    }

    private static ModConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            ModConfig cfg = new ModConfig();
            cfg.save();
            return cfg;
        }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            ModConfig cfg = GSON.fromJson(r, ModConfig.class);
            if (cfg == null) cfg = new ModConfig();
            return cfg;
        } catch (Exception e) {
            LastDoubleLife.LOGGER.error("[LDL] Failed to load config, using defaults: {}", e.getMessage());
            return new ModConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            LastDoubleLife.LOGGER.error("[LDL] Failed to save config: {}", e.getMessage());
        }
    }
}
