package earth.terrarium.argonauts.common.utils;

import com.google.gson.Gson;
import earth.terrarium.argonauts.Argonauts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Argonauts.MOD_ID);
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Path.of("config", Argonauts.MOD_ID, "argonauts-common.json");
    private static final GuildLevel FALLBACK = new GuildLevel(1, 8);

    public static int maxPartyMembers = Argonauts.DEFAULT_MAX_PARTY_MEMBERS;
    public static boolean teleportEnabled = false;
    public static int maxGuildTargets = 128;
    public static Map<Integer, GuildLevel> guildLevels = defaultGuildLevels();

    private Config() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        ConfigData data = read();
        if (data == null) return;
        maxPartyMembers = clamp("maxPartyMembers", data.maxPartyMembers,
            Argonauts.MIN_PARTY_MEMBERS, Argonauts.MAX_PARTY_MEMBERS);
        teleportEnabled = data.teleportEnabled;
        maxGuildTargets = clamp("maxGuildTargets", data.maxGuildTargets, 1, 1024);
        guildLevels = sanitize(data.guildLevels);
        save();
    }

    public static boolean hasLevel(int level) {
        return guildLevels.containsKey(level);
    }

    public static int getMaxTowns(int level) {
        return resolve(level).maxTowns;
    }

    public static int getMaxMembers(int level) {
        return resolve(level).maxMembers;
    }

    private static GuildLevel resolve(int level) {
        GuildLevel exact = guildLevels.get(level);
        if (exact != null) return exact;
        return guildLevels.entrySet().stream()
            .filter(entry -> entry.getKey() < level)
            .max(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .orElse(FALLBACK);
    }

    private static Map<Integer, GuildLevel> sanitize(Map<Integer, GuildLevel> levels) {
        Map<Integer, GuildLevel> sanitized = new LinkedHashMap<>();
        if (levels != null) {
            levels.forEach((level, value) -> {
                if (level == null || level < 1 || value == null || value.maxTowns < 1 || value.maxMembers < 1) {
                    LOGGER.warn("Invalid guild level '{}', skipping", level);
                    return;
                }
                sanitized.put(level, value);
            });
        }
        if (sanitized.isEmpty()) {
            LOGGER.warn("No valid guild levels in config, using defaults");
            return defaultGuildLevels();
        }
        return sanitized;
    }

    private static Map<Integer, GuildLevel> defaultGuildLevels() {
        Map<Integer, GuildLevel> levels = new LinkedHashMap<>();
        levels.put(1, new GuildLevel(1, 8));
        levels.put(2, new GuildLevel(2, 16));
        levels.put(3, new GuildLevel(3, 32));
        return levels;
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
        if (!Files.exists(CONFIG_PATH)) return null;
        try {
            return GSON.fromJson(JsonUtils.stripComments(Files.readString(CONFIG_PATH)), ConfigData.class);
        } catch (Exception e) {
            LOGGER.error("Failed to read config, falling back to defaults", e);
            return null;
        }
    }

    private static void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"maxPartyMembers\": ").append(maxPartyMembers).append(",\n");
        sb.append("  \"teleportEnabled\": ").append(teleportEnabled).append(",\n");
        sb.append("  \"maxGuildTargets\": ").append(maxGuildTargets).append(",\n");
        sb.append("  // Guild levels: maxTowns and maxMembers per level.\n");
        sb.append("  // New levels like 4, 5, etc. can be added; assign a guild's level with /argonauts guild admin level set <guild> <level>.\n");
        sb.append("  \"guildLevels\": ").append(GSON.toJson(guildLevels)).append("\n");
        sb.append("}\n");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, sb.toString());
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public static final class GuildLevel {
        public int maxTowns;
        public int maxMembers;

        GuildLevel() {}

        GuildLevel(int maxTowns, int maxMembers) {
            this.maxTowns = maxTowns;
            this.maxMembers = maxMembers;
        }
    }

    private static final class ConfigData {
        int maxPartyMembers = Argonauts.DEFAULT_MAX_PARTY_MEMBERS;
        boolean teleportEnabled = false;
        int maxGuildTargets = 128;
        Map<Integer, GuildLevel> guildLevels;
    }
}
