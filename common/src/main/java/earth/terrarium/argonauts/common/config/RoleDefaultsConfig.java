package earth.terrarium.argonauts.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.common.utils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RoleDefaultsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(Argonauts.MOD_ID);
    private static final Map<String, String> FILE_NAMES = Map.of(
        Role.OUTSIDER, "role_outsider.toml",
        Role.MEMBER, "role_member.toml",
        Role.ALLY, "role_ally.toml"
    );
    private static final List<String> ROLES = List.of(Role.OUTSIDER, Role.MEMBER, Role.ALLY);
    public static final List<String> CONDITION_PARENTS = List.of(
        "block-break",
        "block-place",
        "block-interactions",
        "entity-interactions",
        "entity-damage",
        "item-pickup",
        "use"
    );
    private static final List<String> PRESET_TARGETS = List.of(
        "block-break/minecraft:dirt",
        "block-place/minecraft:dirt",
        "block-interactions/#minecraft:doors",
        "block-interactions/#minecraft:trapdoors",
        "entity-interactions/minecraft:boat",
        "entity-interactions/minecraft:chest_boat",
        "entity-interactions/minecraft:horse",
        "entity-damage/minecraft:horse",
        "item-pickup/minecraft:dirt",
        "use/#c:foods",
        "use/minecraft:potion",
        "use/minecraft:splash_potion"
    );

    private static final Map<String, Map<String, TriState>> VALUES = new LinkedHashMap<>();
    private static boolean loaded;

    private RoleDefaultsConfig() {
    }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        for (String role : ROLES) {
            VALUES.put(role, load(role));
        }
    }

    public static Map<String, TriState> values(String role) {
        ensureLoaded();
        return VALUES.getOrDefault(role, Map.of());
    }

    public static TriState get(String role, String key) {
        return values(role).getOrDefault(key, TriState.UNDEFINED);
    }

    public static List<RoleTarget> targets() {
        ensureLoaded();
        Set<RoleTarget> targets = new LinkedHashSet<>();
        for (Map<String, TriState> values : VALUES.values()) {
            for (String key : values.keySet()) {
                int index = key.indexOf('/');
                if (index > 0 && index < key.length() - 1) {
                    targets.add(new RoleTarget(key.substring(0, index), key.substring(index + 1)));
                }
            }
        }
        return List.copyOf(targets);
    }

    private static Map<String, TriState> load(String role) {
        Path path = Config.DEFAULT_FOLDER.resolve(FILE_NAMES.get(role));
        if (Files.exists(path)) {
            try {
                return parse(Files.readString(path));
            } catch (Exception e) {
                LOGGER.warn("Failed to read role defaults from {} ({}), using in-memory defaults", path, e.toString());
                return generate(role);
            }
        }

        Map<String, TriState> defaults = generate(role);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, write(role, defaults));
        } catch (Exception e) {
            LOGGER.warn("Failed to write role defaults to {} ({})", path, e.toString());
        }
        return defaults;
    }

    private static Map<String, TriState> parse(String text) {
        Map<String, TriState> values = new LinkedHashMap<>();
        CommentedConfig root = new TomlParser().parse(new StringReader(text));

        Object permissions = root.get("permissions");
        if (permissions instanceof com.electronwill.nightconfig.core.Config table) {
            table.valueMap().forEach((key, value) -> values.put(key, parseState(key, value)));
        }

        Object settings = root.get("settings");
        if (settings instanceof com.electronwill.nightconfig.core.Config table) {
            table.valueMap().forEach((key, value) -> {
                if (value instanceof com.electronwill.nightconfig.core.Config child) {
                    child.valueMap().forEach((childKey, childValue) -> {
                        String name = String.valueOf(childKey);
                        if (name.equals("value")) {
                            values.put(key, parseState(key, childValue));
                        } else {
                            values.put(key + "/" + name, parseState(key + "/" + name, childValue));
                        }
                    });
                } else {
                    values.put(key, parseState(key, value));
                }
            });
        }
        return values;
    }

    private static Map<String, TriState> generate(String role) {
        TriState value = Role.OUTSIDER.equals(role) ? TriState.FALSE : TriState.TRUE;
        Map<String, TriState> values = new LinkedHashMap<>();
        MemberPermissionsApi.API.getGuildPermissions().keySet().forEach(permission -> values.put(permission, value));
        for (MemberSetting setting : MemberSettingsApi.API.getSettings(null)) {
            values.put(settingKey(setting), value);
        }
        if (Argonauts.IS_CLAIMS_LOADED) {
            for (String target : PRESET_TARGETS) {
                values.putIfAbsent(target, value);
            }
        }
        return values;
    }

    private static String settingKey(MemberSetting setting) {
        String id = setting.id();
        int index = id.indexOf('/');
        if (index <= 0) return id;
        String name = setting.name().getString();
        return name.startsWith("#") ? id.substring(0, index) + "/" + name : id;
    }

    private static TriState parseState(String key, Object element) {
        if (element instanceof Boolean bool) {
            return bool ? TriState.TRUE : TriState.FALSE;
        }
        if (!(element instanceof String string)) {
            LOGGER.warn("Invalid value for role default '{}', treating as inherit", key);
            return TriState.UNDEFINED;
        }
        return switch (string.toLowerCase(Locale.ROOT)) {
            case "allow", "true" -> TriState.TRUE;
            case "deny", "false" -> TriState.FALSE;
            case "inherit", "undefined" -> TriState.UNDEFINED;
            default -> {
                LOGGER.warn("Unknown role default value '{}' for '{}', treating as inherit", string, key);
                yield TriState.UNDEFINED;
            }
        };
    }

    private static String write(String role, Map<String, TriState> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Argonauts role defaults for \"").append(role).append("\".\n");
        sb.append("# Applied to new guilds and to missing keys of existing guilds.\n");
        sb.append("# Values: allow, deny, inherit; targets may be ids (minecraft:dirt) or tags (#minecraft:doors).\n");

        Map<String, Entry> groups = group(values);

        sb.append("\n[permissions]\n");
        groups.forEach((key, entry) -> {
            if (!MemberPermissionsApi.API.getGuildPermissions().containsKey(key)) return;
            sb.append(quote(key)).append(" = \"").append(stateName(entry.value)).append("\"\n");
        });

        sb.append("\n[settings]\n");
        groups.forEach((key, entry) -> {
            if (MemberPermissionsApi.API.getGuildPermissions().containsKey(key) || !entry.children.isEmpty()) return;
            sb.append(quote(key)).append(" = \"").append(stateName(entry.value)).append("\"\n");
        });
        groups.forEach((key, entry) -> {
            if (MemberPermissionsApi.API.getGuildPermissions().containsKey(key) || entry.children.isEmpty()) return;
            sb.append("\n[settings.").append(quote(key)).append("]\n");
            if (entry.value != null) {
                sb.append("value = \"").append(stateName(entry.value)).append("\"\n");
            }
            entry.children.forEach((childKey, childValue) ->
                sb.append(quote(childKey)).append(" = \"").append(stateName(childValue)).append("\"\n"));
        });
        return sb.toString();
    }

    private static Map<String, Entry> group(Map<String, TriState> values) {
        Map<String, Entry> groups = new LinkedHashMap<>();
        for (Map.Entry<String, TriState> entry : values.entrySet()) {
            String key = entry.getKey();
            int index = key.indexOf('/');
            if (index > 0) {
                groups.computeIfAbsent(key.substring(0, index), ignored -> new Entry())
                    .children.put(key.substring(index + 1), entry.getValue());
            } else {
                groups.computeIfAbsent(key, ignored -> new Entry()).value = entry.getValue();
            }
        }
        return groups;
    }

    private static String stateName(TriState state) {
        if (state == null || state == TriState.UNDEFINED) return "inherit";
        return state == TriState.TRUE ? "allow" : "deny";
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public record RoleTarget(String parent, String key) {
    }

    private static final class Entry {
        private TriState value;
        private final Map<String, TriState> children = new LinkedHashMap<>();
    }
}
