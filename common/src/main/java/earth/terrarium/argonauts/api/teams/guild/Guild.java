package earth.terrarium.argonauts.api.teams.guild;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.bytecodecs.defaults.MapCodec;
import com.teamresourceful.resourcefullib.common.color.Color;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.MemberStatus;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.Setting;
import earth.terrarium.argonauts.api.teams.settings.types.ColorSettings;
import earth.terrarium.argonauts.api.teams.settings.types.StringSetting;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.argonauts.common.guild.GuildRoleDefaults;
import earth.terrarium.argonauts.common.permissions.Permissions;
import earth.terrarium.argonauts.common.settings.Settings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Guilds are a more permanent form of team. They are persistent across logins and feature role management, setting headquarters, displaying messages of the day, and more. Guilds also integrate with Cadmus for chunk claiming as a team, and Heracles for completing quests together.
 *
 * @param id       The guild ID
 * @param members  A map of members to their corresponding permissions and member status
 * @param settings A map of setting IDs to their corresponding values
 * @param roles    A map of role IDs to their corresponding roles
 */
public record Guild(
    UUID id,
    Map<UUID, Member> members,
    Map<String, Setting<?>> settings,
    Map<String, Role> roles
) implements Team {

    public static final ByteCodec<Guild> BYTE_CODEC = ObjectByteCodec.create(
        ByteCodec.UUID.fieldOf(Guild::id),
        new MapCodec<>(ByteCodec.UUID, Member.BYTE_CODEC).fieldOf(Guild::members),
        new MapCodec<>(ByteCodec.STRING, Setting.BYTE_CODEC).fieldOf(Guild::settings),
        new MapCodec<>(ByteCodec.STRING, Role.BYTE_CODEC).fieldOf(Guild::roles),
        Guild::new
    );

    public Guild(UUID creator, String name) {
        this(UUID.randomUUID(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        this.members.put(creator, new Member(MemberStatus.OWNER, MemberPermissionsApi.API.getGuildPermissions()));
        this.settings.put(Settings.DISPLAY_NAME.id(), new StringSetting(Settings.DISPLAY_NAME.id(), name));
        this.settings.put(Settings.COLOR.id(), new ColorSettings(Settings.COLOR.id(), ModUtils.uuidToColor(this.id)));
        this.roles.putAll(GuildRoleDefaults.create(this));
    }

    @Override
    public Member getOrCreateMember(UUID player) {
        return this.members.computeIfAbsent(player, id -> new Member(MemberStatus.MEMBER, MemberPermissionsApi.API.getGuildPermissions()));
    }

    @Override
    public boolean isPublic() {
        return Settings.PUBLIC.get(this);
    }

    @Override
    public Component displayName() {
        return Component.literal(Settings.DISPLAY_NAME.get(this)).withColor(this.color().getValue());
    }

    @Override
    public Color color() {
        return Settings.COLOR.get(this);
    }

    @Override
    public String type() {
        return "guild";
    }

    /**
     * Gets the allies of the team.
     *
     * @param level the level
     * @return the allies
     */
    public List<Player> allies(Level level) {
        return this.members().entrySet().stream()
            .filter(entry -> entry.getValue().status().isAllied())
            .map(Map.Entry::getKey)
            .map(level::getPlayerByUUID)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Checks if the player is an ally of the team.
     *
     * @param player the player
     * @return if the player is an ally
     */
    public boolean isAllied(UUID player) {
        Member member = this.members().get(player);
        return member != null && member.status().isAllied();
    }

    /**
     * Gets the fake players of the team. Use sparingly.
     *
     * @return the fake players
     */
    public Set<UUID> getFakePlayers() {
        return this.members().entrySet().stream()
            .filter(entry -> entry.getValue().status().isFakePlayer())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    /**
     * Checks if the player is a member or a fake player of the team.
     *
     * @param player the player
     * @return if the player is a member
     */
    public boolean isMemberOrFakePlayer(UUID player) {
        Member member = this.members().get(player);
        return member != null && (member.status().isMember() || member.status().isFakePlayer());
    }

    /**
     * Gets the role that applies to the player. Members and fake players use their assigned role,
     * allies always use the ally role and everyone else uses the all role.
     *
     * @param player the player
     * @return the role id
     */
    public String getRoleId(UUID player) {
        Member member = this.members().get(player);
        if (member == null || member.status().isInvited()) return Role.ALL;
        if (member.status().isAllied()) return Role.ALLY;
        return member.role().isEmpty() ? Role.MEMBER : member.role();
    }

    /**
     * Resolves the value of a permission for the player by walking their role chain, without
     * applying personal overrides.
     *
     * @param player     the player
     * @param permission the permission or setting id
     * @return the resolved value, or {@link TriState#UNDEFINED} if no role sets it
     */
    public TriState getRoleValue(UUID player, String permission) {
        return this.getRoleValue(this.getRoleId(player), permission);
    }

    /**
     * Resolves the value of a permission by walking the role chain, starting at the given role.
     *
     * @param roleId     the role id
     * @param permission the permission or setting id
     * @return the resolved value, or {@link TriState#UNDEFINED} if no role sets it
     */
    public TriState getRoleValue(String roleId, String permission) {
        Set<String> visited = new HashSet<>();
        String current = roleId;
        while (current != null && !current.isEmpty() && visited.add(current)) {
            Role role = this.roles().get(current);
            if (role == null) break;
            TriState value = role.override(permission);
            if (value != TriState.UNDEFINED) return value;
            current = role.parent();
        }
        return TriState.UNDEFINED;
    }

    /**
     * Resolves the value of a permission for a member, applying personal overrides before the
     * role chain.
     *
     * @param member     the member
     * @param permission the permission
     * @return the resolved value
     */
    public TriState getPermission(Member member, String permission) {
        TriState personal = member.permissionOverride(permission);
        if (personal != TriState.UNDEFINED) return personal;
        return this.getRoleValue(member.role().isEmpty() ? Role.MEMBER : member.role(), permission);
    }

    /**
     * Checks if setting the parent of a role would create a cycle.
     *
     * @param roleId   the role id
     * @param parentId the parent role id
     * @return if the parent would create a cycle
     */
    public boolean wouldCreateRoleCycle(String roleId, String parentId) {
        Set<String> visited = new HashSet<>();
        String current = parentId;
        while (current != null && !current.isEmpty() && visited.add(current)) {
            if (current.equals(roleId)) return true;
            Role role = this.roles().get(current);
            if (role == null) return false;
            current = role.parent();
        }
        return false;
    }

    @Override
    public boolean hasPermission(UUID player, String permission) {
        Member member = this.members().get(player);
        if (member == null || !member.status().isMember()) return false;
        if (member.isOwner()) return true;
        if (this.getPermission(member, Permissions.OPERATOR) == TriState.TRUE) return true;
        return this.getPermission(member, permission) == TriState.TRUE;
    }
}
