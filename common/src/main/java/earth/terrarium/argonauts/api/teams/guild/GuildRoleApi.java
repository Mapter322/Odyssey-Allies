package earth.terrarium.argonauts.api.teams.guild;

import earth.terrarium.argonauts.api.ApiHelper;
import net.minecraft.world.level.Level;

import java.util.UUID;

public interface GuildRoleApi {

    GuildRoleApi API = ApiHelper.load(GuildRoleApi.class);

    /**
     * Creates or modifies a role.
     *
     * @param level the level
     * @param guild the guild
     * @param role  the role
     */
    void modifyRole(Level level, Guild guild, Role role);

    /**
     * Removes a role. Members with this role are reassigned to the member role and child roles are
     * re-parented to the parent of the removed role.
     *
     * @param level  the level
     * @param guild  the guild
     * @param roleId the role ID
     */
    void removeRole(Level level, Guild guild, String roleId);

    /**
     * Sets the role of a member.
     *
     * @param level    the level
     * @param guild    the guild
     * @param playerId the player ID
     * @param roleId   the role ID
     */
    void modifyMemberRole(Level level, Guild guild, UUID playerId, String roleId);
}
