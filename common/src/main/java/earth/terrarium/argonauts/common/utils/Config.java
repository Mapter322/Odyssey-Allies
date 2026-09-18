package earth.terrarium.argonauts.common.utils;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import earth.terrarium.argonauts.Argonauts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Argonauts.MOD_ID);
    private static final Path CONFIG_PATH = Path.of("config", "odyssey", "argonauts-server.toml");
    private static final GuildLevel FALLBACK = new GuildLevel(1, 8, 50, 10, 8);

    public static final Path DEFAULT_FOLDER = Path.of("config", "odyssey", "default");

    public static int maxPartyMembers = Argonauts.DEFAULT_MAX_PARTY_MEMBERS;
    public static boolean teleportEnabled = false;
    public static int maxGuildConditions = 128;
    public static int maxChatHistory = 1000;
    public static Map<Integer, GuildLevel> guildLevels = defaultGuildLevels();

    private Config() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        int partyMembers;
        boolean teleport;
        int guildConditions;
        int chatHistory;
        Map<Integer, GuildLevel> levels;
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            CommentedConfig root = new TomlParser().parse(reader);
            partyMembers = root.getIntOrElse("party.max-members", maxPartyMembers);
            teleport = root.getOrElse("party.teleport-enabled", teleportEnabled);
            guildConditions = root.getIntOrElse("guild.conditions", maxGuildConditions);
            chatHistory = root.getIntOrElse("chat.max-history", maxChatHistory);
            levels = parseLevels(root);
        } catch (Exception e) {
            LOGGER.error("Failed to read config, falling back to defaults", e);
            return;
        }

        maxPartyMembers = clamp("maxPartyMembers", partyMembers, Argonauts.MIN_PARTY_MEMBERS, Argonauts.MAX_PARTY_MEMBERS);
        teleportEnabled = teleport;
        maxGuildConditions = clamp("maxGuildConditions", guildConditions, 1, 1024);
        maxChatHistory = clamp("maxChatHistory", chatHistory, 1, 10000);
        if (!levels.isEmpty()) guildLevels = sanitize(levels);
        save();
    }

    private static Map<Integer, GuildLevel> parseLevels(CommentedConfig root) {
        Map<Integer, GuildLevel> parsed = new LinkedHashMap<>();
        Object value = root.get("guild-levels");
        if (!(value instanceof List<?> list)) return parsed;
        for (Object item : list) {
            if (!(item instanceof com.electronwill.nightconfig.core.Config table)) {
                LOGGER.warn("Invalid guild level entry, skipping");
                continue;
            }
            int number = table.getIntOrElse("level", 0);
            if (number < 1) {
                LOGGER.warn("Invalid guild level entry without a positive level, skipping");
                continue;
            }
            parsed.put(number, new GuildLevel(
                table.getIntOrElse("max-towns", 1),
                table.getIntOrElse("max-members", 1),
                table.getIntOrElse("max-claims", 0),
                table.getIntOrElse("max-forceloads", 0),
                table.getIntOrElse("max-outpost-chunks", 0)
            ));
        }
        return parsed;
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

    public static int getMaxClaims(int level) {
        return Math.max(0, resolve(level).maxClaims);
    }

    public static int getMaxForceloads(int level) {
        return Math.max(0, resolve(level).maxForceloads);
    }

    public static int getMaxOutpostChunks(int level) {
        return Math.max(0, resolve(level).maxOutpostChunks);
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
        levels.put(1, new GuildLevel(1, 8, 50, 10, 8));
        levels.put(2, new GuildLevel(2, 16, 120, 25, 16));
        levels.put(3, new GuildLevel(3, 32, 300, 60, 24));
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

    private static void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Argonauts server configuration.\n");
        sb.append("# Generated at config/odyssey/argonauts-server.toml; edit values below and restart the server.\n\n");

        sb.append("[party]\n");
        sb.append("# Maximum number of party members.\n");
        sb.append("max-members = ").append(maxPartyMembers).append("\n\n");
        sb.append("# Whether party teleportation is enabled.\n");
        sb.append("teleport-enabled = ").append(teleportEnabled).append("\n\n");

        sb.append("[guild]\n");
        sb.append("# Maximum number of conditions a guild can have.\n");
        sb.append("conditions = ").append(maxGuildConditions).append("\n\n");

        sb.append("[chat]\n");
        sb.append("# Maximum number of messages stored per guild/party chat history.\n");
        sb.append("max-history = ").append(maxChatHistory).append("\n\n");

        sb.append("# Guild levels: towns, members, claims, forceloads and outpost chunks per level (0 means no level cap).\n");
        sb.append("# New levels like 4, 5, etc. can be added; assign a guild's level with\n");
        sb.append("# /argonauts guild admin level set <guild> <level>.\n");
        guildLevels.forEach((level, data) -> {
            sb.append("\n[[guild-levels]]\n");
            sb.append("level = ").append(level).append("\n");
            sb.append("max-towns = ").append(data.maxTowns).append("\n");
            sb.append("max-members = ").append(data.maxMembers).append("\n");
            sb.append("max-claims = ").append(data.maxClaims).append("\n");
            sb.append("max-forceloads = ").append(data.maxForceloads).append("\n");
            sb.append("max-outpost-chunks = ").append(data.maxOutpostChunks).append("\n");
        });

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
        public int maxClaims;
        public int maxForceloads;
        public int maxOutpostChunks;

        GuildLevel(int maxTowns, int maxMembers, int maxClaims, int maxForceloads, int maxOutpostChunks) {
            this.maxTowns = maxTowns;
            this.maxMembers = maxMembers;
            this.maxClaims = maxClaims;
            this.maxForceloads = maxForceloads;
            this.maxOutpostChunks = maxOutpostChunks;
        }
    }
}
