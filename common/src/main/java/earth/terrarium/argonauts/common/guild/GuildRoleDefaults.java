package earth.terrarium.argonauts.common.guild;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;

public final class GuildRoleDefaults {

    private GuildRoleDefaults() {
    }

    /**
     * Creates the default roles of a guild. The member and ally roles inherit from all, and are
     * seeded with the current default permissions to preserve the behavior of a flat hierarchy.
     */
    public static Map<String, Role> create(Team team) {
        Map<String, Role> roles = new HashMap<>();
        roles.put(Role.ALL, new Role(Role.ALL, ""));

        Object2ObjectOpenHashMap<String, TriState> memberOverrides = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, TriState> allyOverrides = new Object2ObjectOpenHashMap<>();

        MemberPermissionsApi.API.getGuildPermissions().forEach((permission, enabled) -> {
            if (enabled) {
                memberOverrides.put(permission, TriState.TRUE);
                allyOverrides.put(permission, TriState.TRUE);
            }
        });
        MemberSettingsApi.API.getSettings(team).forEach(setting -> {
            memberOverrides.put(setting.id(), TriState.TRUE);
            allyOverrides.put(setting.id(), TriState.TRUE);
        });

        roles.put(Role.MEMBER, new Role(Role.MEMBER, Role.ALL, memberOverrides));
        roles.put(Role.ALLY, new Role(Role.ALLY, Role.ALL, allyOverrides));
        return roles;
    }

    public static boolean isDefaultRole(String id) {
        return Role.ALL.equals(id) || Role.MEMBER.equals(id) || Role.ALLY.equals(id);
    }
}
