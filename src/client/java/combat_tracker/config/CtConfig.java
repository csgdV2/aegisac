package combat_tracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CtConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("combat_tracker/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static CtConfig instance;

    public TimingWindow window = new TimingWindow();

    public static CtConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("combat_tracker");
    }

    private static Path configFile() {
        return configDir().resolve("config.json");
    }

    public static CtConfig load() {
        Path file = configFile();
        try {
            if (Files.exists(file)) {
                CtConfig cfg = GSON.fromJson(Files.readString(file), CtConfig.class);
                if (cfg != null) {
                    if (cfg.window == null) {
                        cfg.window = new TimingWindow();
                    }
                    return cfg;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load config, using defaults", e);
        }
        CtConfig def = new CtConfig();
        def.saveInternal();
        return def;
    }

    public static void save() {
        if (instance != null) {
            instance.saveInternal();
        }
    }

    private void saveInternal() {
        try {
            Files.createDirectories(configDir());
            Files.writeString(configFile(), GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.warn("Failed to save config", e);
        }
    }
}
