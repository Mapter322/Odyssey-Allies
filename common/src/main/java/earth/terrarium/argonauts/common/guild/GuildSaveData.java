package earth.terrarium.argonauts.common.guild;

import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.MemberStatus;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.Setting;
import earth.terrarium.argonauts.api.teams.settings.TeamSettingsApi;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class GuildSaveData extends SaveHandler {

    private static final GuildSaveData CLIENT_SIDE = new GuildSaveData();
    private final Map<UUID, Guild> guilds = new HashMap<>();
    private final Map<UUID, Guild> guildsByPlayer = new HashMap<>();

    @Override
    public void loadData(CompoundTag tag) {
        CompoundTag guildsTag = tag.getCompound("guilds");
        guildsTag.getAllKeys().forEach(id -> {
            CompoundTag guildTag = guildsTag.getCompound(id);
            Map<UUID, Member> members = new HashMap<>();
            Map<String, Setting<?>> settings = new HashMap<>();
            Map<String, Role> roles = new HashMap<>();

            CompoundTag membersTag = guildTag.getCompound("members");
            membersTag.getAllKeys().forEach(memberId -> {
                UUID uuid = UUID.fromString(memberId);
                CompoundTag memberTag = membersTag.getCompound(memberId);
                MemberStatus status = MemberStatus.valueOf(memberTag.getString("status").toUpperCase(Locale.ROOT));
                CompoundTag permissionsTag = memberTag.getCompound("permissions");
                Object2BooleanMap<String> permissions = new Object2BooleanOpenHashMap<>();
                permissionsTag.getAllKeys().forEach(permission -> permissions.put(permission, permissionsTag.getBoolean(permission)));
                String role = memberTag.contains("role")
                    ? memberTag.getString("role")
                    : (status.isAllied() ? Role.ALLY : Role.MEMBER);
                Object2ObjectMap<String, TriState> overrides = memberTag.contains("overrides")
                    ? readOverrides(memberTag.getCompound("overrides"))
                    : migratePermissions(permissions);
                Member member = new Member(status, permissions, memberTag.getString("name"), role, overrides);
                members.put(uuid, member);
            });

            CompoundTag settingsTag = guildTag.getCompound("settings");
            settingsTag.getAllKeys().forEach(settingId -> {
                Setting<?> setting = TeamSettingsApi.API.getDefaultValue(settingId).deserialize(settingsTag);
                settings.put(settingId, setting);
            });

            CompoundTag rolesTag = guildTag.getCompound("roles");
            rolesTag.getAllKeys().forEach(roleId -> {
                CompoundTag roleTag = rolesTag.getCompound(roleId);
                roles.put(roleId, new Role(roleId, roleTag.getString("parent"), readOverrides(roleTag.getCompound("overrides"))));
            });

            Guild guild = new Guild(UUID.fromString(id), members, settings, roles);
            if (guild.roles().isEmpty()) {
                guild.roles().putAll(GuildRoleDefaults.create(guild));
            }
            this.guilds.put(guild.id(), guild);
            members.forEach((memberId, member) -> {
                if (member.status().isMember()) {
                    this.guildsByPlayer.put(memberId, guild);
                }
            });
        });
    }

    @Override
    public void saveData(CompoundTag tag) {
        CompoundTag guildsTag = new CompoundTag();
        this.guilds.forEach((id, guild) -> {
            CompoundTag guildTag = new CompoundTag();

            CompoundTag membersTag = new CompoundTag();
            guild.members().forEach((memberId, member) -> {
                if (!member.status().isInvited()) {
                    CompoundTag memberTag = new CompoundTag();
                    memberTag.putString("status", member.status().name().toLowerCase(Locale.ROOT));
                    memberTag.putString("name", member.name());
                    CompoundTag permissionsTag = new CompoundTag();
                    member.permissions().forEach(permissionsTag::putBoolean);
                    memberTag.put("permissions", permissionsTag);
                    memberTag.putString("role", member.role());
                    CompoundTag overridesTag = new CompoundTag();
                    writeOverrides(overridesTag, member.permissionOverrides());
                    memberTag.put("overrides", overridesTag);
                    membersTag.put(memberId.toString(), memberTag);
                }
            });
            guildTag.put("members", membersTag);

            CompoundTag settingsTag = new CompoundTag();
            guild.settings().forEach((name, setting) -> setting.serialize(settingsTag));
            guildTag.put("settings", settingsTag);

            CompoundTag rolesTag = new CompoundTag();
            guild.roles().forEach((roleId, role) -> {
                CompoundTag roleTag = new CompoundTag();
                roleTag.putString("parent", role.parent());
                CompoundTag overridesTag = new CompoundTag();
                writeOverrides(overridesTag, role.overrides());
                roleTag.put("overrides", overridesTag);
                rolesTag.put(roleId, roleTag);
            });
            guildTag.put("roles", rolesTag);

            guildsTag.put(id.toString(), guildTag);
        });
        tag.put("guilds", guildsTag);
    }

    private static Object2ObjectMap<String, TriState> migratePermissions(Object2BooleanMap<String> permissions) {
        Object2ObjectMap<String, TriState> overrides = new Object2ObjectOpenHashMap<>();
        Object2BooleanMap<String> defaults = MemberPermissionsApi.API.getGuildPermissions();
        permissions.forEach((permission, value) -> {
            if (value != defaults.getBoolean(permission)) {
                overrides.put(permission, value ? TriState.TRUE : TriState.FALSE);
            }
        });
        return overrides;
    }

    private static Object2ObjectMap<String, TriState> readOverrides(CompoundTag tag) {
        Object2ObjectMap<String, TriState> overrides = new Object2ObjectOpenHashMap<>();
        tag.getAllKeys().forEach(id -> overrides.put(id, readState(tag.getString(id))));
        return overrides;
    }

    private static TriState readState(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "allow", "true" -> TriState.TRUE;
            case "deny", "false" -> TriState.FALSE;
            default -> TriState.UNDEFINED;
        };
    }

    private static void writeOverrides(CompoundTag tag, Map<String, TriState> overrides) {
        overrides.forEach((id, state) -> {
            if (state != TriState.UNDEFINED) {
                tag.putString(id, state.name().toLowerCase(Locale.ROOT));
            }
        });
    }

    public static GuildSaveData read(Level level) {
        return read(level, HandlerType.create(CLIENT_SIDE, GuildSaveData::new), "argonauts_guilds");
    }

    public Map<UUID, Guild> guilds() {
        return this.guilds;
    }

    public Map<UUID, Guild> guildsByPlayer() {
        return this.guildsByPlayer;
    }
}
