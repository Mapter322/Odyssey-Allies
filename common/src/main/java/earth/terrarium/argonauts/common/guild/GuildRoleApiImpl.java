package earth.terrarium.argonauts.common.guild;

import earth.terrarium.argonauts.api.events.ArgonautsEvents;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildRoleApi;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.common.network.NetworkHandler;
import earth.terrarium.argonauts.common.network.packets.ClientboundModifyGuildMemberRolePacket;
import earth.terrarium.argonauts.common.network.packets.ClientboundRemoveGuildRolePacket;
import earth.terrarium.argonauts.common.network.packets.ClientboundUpdateGuildRolePacket;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuildRoleApiImpl implements GuildRoleApi {

    @Override
    public void modifyRole(Level level, Guild guild, Role role) {
        guild.roles().put(role.id(), role);
        if (level instanceof ServerLevel serverLevel) {
            GuildSaveData.read(level).setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundUpdateGuildRolePacket(guild.id(), role), serverLevel.getServer());
        }
        ArgonautsEvents.GuildChangedEvent.fire(level, guild);
    }

    @Override
    public void removeRole(Level level, Guild guild, String roleId) {
        Role removed = guild.roles().remove(roleId);
        if (removed == null) return;

        ObjectSet<String> conditions = guild.conditions().remove(roleId);
        guild.removedConditions().remove(roleId);
        if (conditions != null) {
            for (String condition : conditions) {
                if (guild.getConditions().contains(condition)) continue;
                guild.members().values().forEach(member -> {
                    member.setPermissionOverride(condition, TriState.UNDEFINED);
                    member.permissions().remove(condition);
                });
            }
        }

        List<Role> reParented = new ArrayList<>();
        guild.roles().values().forEach(role -> {
            if (role.parent().equals(roleId)) {
                role.setParent(removed.parent());
                reParented.add(role);
            }
        });

        List<UUID> reassigned = new ArrayList<>();
        guild.members().forEach((playerId, member) -> {
            if (member.role().equals(roleId)) {
                member.setRole(Role.MEMBER);
                reassigned.add(playerId);
            }
        });

        if (level instanceof ServerLevel serverLevel) {
            GuildSaveData.read(level).setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundRemoveGuildRolePacket(guild.id(), roleId), serverLevel.getServer());
            reParented.forEach(role -> NetworkHandler.sendToAllClientPlayers(new ClientboundUpdateGuildRolePacket(guild.id(), role), serverLevel.getServer()));
            reassigned.forEach(playerId -> NetworkHandler.sendToAllClientPlayers(new ClientboundModifyGuildMemberRolePacket(guild.id(), playerId, Role.MEMBER), serverLevel.getServer()));
        }
        ArgonautsEvents.GuildChangedEvent.fire(level, guild);
    }

    @Override
    public void modifyMemberRole(Level level, Guild guild, UUID playerId, String roleId) {
        Member member = guild.members().get(playerId);
        if (member == null) return;
        member.setRole(roleId);
        if (level instanceof ServerLevel serverLevel) {
            GuildSaveData.read(level).setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundModifyGuildMemberRolePacket(guild.id(), playerId, roleId), serverLevel.getServer());
        }
        ArgonautsEvents.GuildChangedEvent.fire(level, guild);
    }
}
