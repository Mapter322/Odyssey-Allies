package earth.terrarium.argonauts.common.guild;

import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;

public final class GuildRoleDefaults {

    private GuildRoleDefaults() {
    }

    public static Map<String, Role> create(Team team) {
        Object2ObjectOpenHashMap<String, TriState> allOverrides = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, TriState> memberOverrides = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, TriState> allyOverrides = new Object2ObjectOpenHashMap<>();

        for (String permission : MemberPermissionsApi.API.getGuildPermissions().keySet()) {
            allOverrides.put(permission, TriState.FALSE);
            memberOverrides.put(permission, TriState.TRUE);
            allyOverrides.put(permission, TriState.TRUE);
        }
        for (MemberSetting setting : MemberSettingsApi.API.getSettings(team)) {
            allOverrides.put(setting.id(), TriState.FALSE);
            memberOverrides.put(setting.id(), TriState.TRUE);
            allyOverrides.put(setting.id(), TriState.TRUE);
        }

        Map<String, Role> roles = new HashMap<>();
        roles.put(Role.ALL, new Role(Role.ALL, "", allOverrides));
        roles.put(Role.MEMBER, new Role(Role.MEMBER, Role.ALL, memberOverrides));
        roles.put(Role.ALLY, new Role(Role.ALLY, Role.ALL, allyOverrides));
        return roles;
    }

    public static boolean applyMissingDefaults(Team team, Map<String, Role> roles) {
        Role all = roles.get(Role.ALL);
        Role member = roles.get(Role.MEMBER);
        Role ally = roles.get(Role.ALLY);
        if (all == null) return false;

        boolean changed = false;
        for (String permission : MemberPermissionsApi.API.getGuildPermissions().keySet()) {
            changed |= apply(all, permission, TriState.FALSE);
            changed |= apply(member, permission, TriState.TRUE);
            changed |= apply(ally, permission, TriState.TRUE);
        }
        for (MemberSetting setting : MemberSettingsApi.API.getSettings(team)) {
            changed |= apply(all, setting.id(), TriState.FALSE);
            changed |= apply(member, setting.id(), TriState.TRUE);
            changed |= apply(ally, setting.id(), TriState.TRUE);
        }
        return changed;
    }

    private static boolean apply(Role role, String key, TriState state) {
        if (role == null || role.override(key) != TriState.UNDEFINED) return false;
        role.setOverride(key, state);
        return true;
    }

    public static boolean isDefaultRole(String id) {
        return Role.ALL.equals(id) || Role.MEMBER.equals(id) || Role.ALLY.equals(id);
    }
}
