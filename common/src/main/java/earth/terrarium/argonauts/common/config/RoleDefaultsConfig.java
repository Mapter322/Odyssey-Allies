package earth.terrarium.argonauts.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Path CONFIG_DIR = Path.of("config", Argonauts.MOD_ID);
    private static final Map<String, String> FILE_NAMES = Map.of(
        Role.ALL, "allsettingsdefault.json",
        Role.MEMBER, "membersettingsdefault.json",
        Role.ALLY, "allysettingsdefault.json"
    );
    private static final List<String> ROLES = List.of(Role.ALL, Role.MEMBER, Role.ALLY);
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
        Path path = CONFIG_DIR.resolve(FILE_NAMES.get(role));
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
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(path, write(role, defaults));
        } catch (Exception e) {
            LOGGER.warn("Failed to write role defaults to {} ({})", path, e.toString());
        }
        return defaults;
    }

    private static Map<String, TriState> parse(String text) {
        Map<String, TriState> values = new LinkedHashMap<>();
        JsonObject root = JsonParser.parseString(stripComments(text)).getAsJsonObject();

        JsonObject permissions = asObject(root.get("permissions"), "permissions");
        if (permissions != null) {
            for (Map.Entry<String, JsonElement> entry : permissions.entrySet()) {
                values.put(entry.getKey(), parseState(entry.getKey(), entry.getValue()));
            }
        }

        JsonObject settings = asObject(root.get("settings"), "settings");
        if (settings != null) {
            for (Map.Entry<String, JsonElement> entry : settings.entrySet()) {
                String parent = entry.getKey();
                JsonElement element = entry.getValue();
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    if (object.has("value")) {
                        values.put(parent, parseState(parent, object.get("value")));
                    }
                    for (Map.Entry<String, JsonElement> child : object.entrySet()) {
                        if (child.getKey().equals("value")) continue;
                        values.put(parent + "/" + child.getKey(), parseState(parent + "/" + child.getKey(), child.getValue()));
                    }
                } else {
                    values.put(parent, parseState(parent, element));
                }
            }
        }
        return values;
    }

    private static Map<String, TriState> generate(String role) {
        TriState value = Role.ALL.equals(role) ? TriState.FALSE : TriState.TRUE;
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

    private static TriState parseState(String key, JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            LOGGER.warn("Invalid value for role default '{}', treating as inherit", key);
            return TriState.UNDEFINED;
        }
        return switch (element.getAsString().toLowerCase(Locale.ROOT)) {
            case "allow", "true" -> TriState.TRUE;
            case "deny", "false" -> TriState.FALSE;
            case "inherit", "undefined" -> TriState.UNDEFINED;
            default -> {
                LOGGER.warn("Unknown role default value '{}' for '{}', treating as inherit", element.getAsString(), key);
                yield TriState.UNDEFINED;
            }
        };
    }

    private static String write(String role, Map<String, TriState> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Argonauts role defaults for \"").append(role).append("\".\n");
        sb.append("// Applied to new guilds and to missing keys of existing guilds.\n");
        sb.append("// Values: allow, deny, inherit; targets may be ids (minecraft:dirt) or tags (#minecraft:doors).\n");
        sb.append("{\n");

        Map<String, Entry> groups = group(values);
        boolean first;

        sb.append("  \"permissions\": {");
        first = true;
        for (Map.Entry<String, Entry> group : groups.entrySet()) {
            if (!MemberPermissionsApi.API.getGuildPermissions().containsKey(group.getKey())) continue;
            sb.append(first ? "\n" : ",\n");
            first = false;
            sb.append("    \"").append(group.getKey()).append("\": \"").append(stateName(group.getValue().value)).append("\"");
        }
        sb.append(first ? "},\n" : "\n  },\n");

        sb.append("  \"settings\": {");
        first = true;
        for (Map.Entry<String, Entry> group : groups.entrySet()) {
            if (MemberPermissionsApi.API.getGuildPermissions().containsKey(group.getKey())) continue;
            sb.append(first ? "\n" : ",\n");
            first = false;
            Entry entry = group.getValue();
            if (entry.children.isEmpty()) {
                sb.append("    \"").append(group.getKey()).append("\": \"").append(stateName(entry.value)).append("\"");
                continue;
            }
            sb.append("    \"").append(group.getKey()).append("\": {\n");
            if (entry.value != null) {
                sb.append("      \"value\": \"").append(stateName(entry.value)).append("\",\n");
            }
            int index = 0;
            for (Map.Entry<String, TriState> child : entry.children.entrySet()) {
                sb.append("      \"").append(child.getKey()).append("\": \"").append(stateName(child.getValue())).append("\"");
                sb.append(++index < entry.children.size() ? ",\n" : "\n");
            }
            sb.append("    }");
        }
        sb.append("\n  }\n}\n");
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

    private static JsonObject asObject(JsonElement element, String key) {
        if (element == null) return null;
        if (element.isJsonObject()) return element.getAsJsonObject();
        LOGGER.warn("Role defaults section '{}' is not an object, skipping", key);
        return null;
    }

    private static String stripComments(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                sb.append(c);
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                sb.append(c);
                continue;
            }
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                while (i < text.length() && text.charAt(i) != '\n') i++;
                if (i < text.length()) sb.append('\n');
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public record RoleTarget(String parent, String key) {
    }

    private static final class Entry {
        private TriState value;
        private final Map<String, TriState> children = new LinkedHashMap<>();
    }
}
