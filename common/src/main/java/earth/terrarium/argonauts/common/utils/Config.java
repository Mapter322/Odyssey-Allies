package earth.terrarium.argonauts.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import earth.terrarium.argonauts.Argonauts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Argonauts.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config", Argonauts.MOD_ID + ".json");

    public static int maxGuildMembers = Argonauts.DEFAULT_MAX_GUILD_MEMBERS;
    public static int maxPartyMembers = Argonauts.DEFAULT_MAX_PARTY_MEMBERS;
    public static boolean teleportEnabled = false;

    private Config() {}

    public static void load() {
        ConfigData data = read();
        maxGuildMembers = clamp("maxGuildMembers", data.maxGuildMembers,
            Argonauts.MIN_GUILD_MEMBERS, Argonauts.MAX_GUILD_MEMBERS);
        maxPartyMembers = clamp("maxPartyMembers", data.maxPartyMembers,
            Argonauts.MIN_PARTY_MEMBERS, Argonauts.MAX_PARTY_MEMBERS);
        teleportEnabled = data.teleportEnabled;
        if (!Files.exists(CONFIG_PATH)) {
            save();
        }
    }

    private static int clamp(String key, int value, int min, int max) {
        int clamped = Math.max(min, Math.min(max, value));
        if (value != clamped) {
            LOGGER.warn("Config '{}' value {} is out of range [{}, {}], clamped to {}",
                key, value, min, max, clamped);
        }
        return clamped;
    }

    private static ConfigData read() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                return GSON.fromJson(Files.readString(CONFIG_PATH), ConfigData.class);
            } catch (Exception e) {
                LOGGER.error("Failed to read config, falling back to defaults", e);
            }
        }
        return new ConfigData();
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(new ConfigData(maxGuildMembers, maxPartyMembers, teleportEnabled)));
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static final class ConfigData {
        int maxGuildMembers = Argonauts.DEFAULT_MAX_GUILD_MEMBERS;
        int maxPartyMembers = Argonauts.DEFAULT_MAX_PARTY_MEMBERS;
        boolean teleportEnabled = false;

        ConfigData() {}

        ConfigData(int maxGuildMembers, int maxPartyMembers, boolean teleportEnabled) {
            this.maxGuildMembers = maxGuildMembers;
            this.maxPartyMembers = maxPartyMembers;
            this.teleportEnabled = teleportEnabled;
        }
    }
}
