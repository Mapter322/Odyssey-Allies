package earth.terrarium.argonauts.common.guild;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.common.config.RoleDefaultsConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GuildRoleDefaults {

    private GuildRoleDefaults() {
    }

    public static Map<String, Role> create(Team team) {
        Map<String, Role> roles = new HashMap<>();
        roles.put(Role.ALL, createRole(Role.ALL, "", RoleDefaultsConfig.values(Role.ALL)));
        roles.put(Role.MEMBER, createRole(Role.MEMBER, Role.ALL, RoleDefaultsConfig.values(Role.MEMBER)));
        roles.put(Role.ALLY, createRole(Role.ALLY, Role.ALL, RoleDefaultsConfig.values(Role.ALLY)));
        return roles;
    }

    public static boolean applyMissingDefaults(Team team, Map<String, Role> roles) {
        if (roles.get(Role.ALL) == null) return false;

        boolean changed = false;
        changed |= applyValues(team, roles.get(Role.ALL), RoleDefaultsConfig.values(Role.ALL));
        changed |= applyValues(team, roles.get(Role.MEMBER), RoleDefaultsConfig.values(Role.MEMBER));
        changed |= applyValues(team, roles.get(Role.ALLY), RoleDefaultsConfig.values(Role.ALLY));
        return changed;
    }

    private static Role createRole(String id, String parent, Map<String, TriState> values) {
        Object2ObjectOpenHashMap<String, TriState> overrides = new Object2ObjectOpenHashMap<>();
        values.forEach((key, state) -> {
            if (state != TriState.UNDEFINED) {
                overrides.put(key, state);
            }
        });
        return new Role(id, parent, overrides);
    }

    private static boolean applyValues(Team team, Role role, Map<String, TriState> values) {
        if (role == null) return false;
        Set<String> removed = team instanceof Guild guild ? guild.getRemovedConditions(role.id()) : Set.of();
        boolean changed = false;
        for (Map.Entry<String, TriState> entry : values.entrySet()) {
            if (entry.getValue() == TriState.UNDEFINED) continue;
            if (removed.contains(entry.getKey())) continue;
            changed |= apply(role, entry.getKey(), entry.getValue());
        }
        return changed;
    }

    private static boolean apply(Role role, String key, TriState state) {
        if (role.override(key) != TriState.UNDEFINED) return false;
        role.setOverride(key, state);
        return true;
    }

    public static boolean isDefaultRole(String id) {
        return Role.ALL.equals(id) || Role.MEMBER.equals(id) || Role.ALLY.equals(id);
    }
}
