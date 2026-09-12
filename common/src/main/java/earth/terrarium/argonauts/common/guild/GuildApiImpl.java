package earth.terrarium.argonauts.common.guild;


import earth.terrarium.argonauts.api.events.ArgonautsEvents;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.MemberStatus;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.settings.Setting;
import com.teamresourceful.resourcefullib.common.utils.TriState;

import earth.terrarium.argonauts.common.network.NetworkHandler;
import earth.terrarium.argonauts.common.network.packets.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GuildApiImpl implements GuildApi {

    private static void resolveMemberNames(ServerLevel serverLevel, Map<UUID, Member> members) {
        var cache = serverLevel.getServer().getProfileCache();
        if (cache == null) return;
        members.forEach((uuid, member) -> {
            if (member.name().isEmpty()) {
                cache.get(uuid).ifPresent(profile -> member.setName(profile.getName()));
            }
        });
    }

    private static void resolveMemberName(ServerLevel serverLevel, Map<UUID, Member> members, UUID playerId) {
        var member = members.get(playerId);
        if (member != null && member.name().isEmpty()) {
            var cache = serverLevel.getServer().getProfileCache();
            if (cache != null) {
                cache.get(playerId).ifPresent(profile -> member.setName(profile.getName()));
            }
        }
    }

    @Override
    public void create(Level level, Guild guild) {
        var data = GuildSaveData.read(level);
        data.guilds().put(guild.id(), guild);
        guild.members().forEach((memberId, member) -> {
            if (member.status().isMember()) {
                data.guildsByPlayer().put(memberId, guild);
            }
        });
        if (level instanceof ServerLevel serverLevel) {
            resolveMemberNames(serverLevel, guild.members());
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundAddGuildPacket(guild), serverLevel.getServer());
        }
        ArgonautsEvents.CreateGuildEvent.fire(level, guild);
    }

    @Override
    public void disband(Level level, Guild guild) {
        var data = GuildSaveData.read(level);
        data.guilds().remove(guild.id());
        guild.members().keySet().forEach(data.guildsByPlayer()::remove);
        if (level instanceof ServerLevel serverLevel) {
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundRemoveGuildPacket(guild.id()), serverLevel.getServer());
        }
        ArgonautsEvents.RemoveGuildEvent.fire(level, guild);
    }

    @Override
    public void join(Level level, Guild guild, UUID playerId) {
        this.modifyMember(level, guild, playerId, MemberStatus.MEMBER);
    }

    @Override
    public void leave(Level level, Guild guild, UUID playerId) {
        var data = GuildSaveData.read(level);
        if (guild.members().get(playerId).status().isMember()) {
            data.guildsByPlayer().remove(playerId);
        }
        guild.members().remove(playerId);
        if (level instanceof ServerLevel serverLevel) {
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundLeaveGuildPacket(guild.id(), playerId), serverLevel.getServer());
        }
        ArgonautsEvents.RemoveGuildMemberEvent.fire(level, guild, playerId);
    }

    @Override
    public void modifyMember(Level level, Guild guild, UUID playerId, MemberStatus status) {
        var data = GuildSaveData.read(level);
        guild.getOrCreateMember(playerId).setStatus(status);
        if (status.isMember()) {
            data.guildsByPlayer().put(playerId, guild);
        }
        if (level instanceof
            ServerLevel serverLevel) {
            resolveMemberName(serverLevel, guild.members(), playerId);
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundModifyGuildMemberPacket(guild.id(), playerId, status), serverLevel.getServer());
        }
        ArgonautsEvents.ModifyGuildMemberEvent.fire(level, guild, playerId, status);
    }

    @Override
    public void modifyPermission(Level level, Guild guild, UUID playerId, String permission, TriState value) {
        var data = GuildSaveData.read(level);
        Member member = guild.getOrCreateMember(playerId);
        member.setPermissionOverride(permission, value);
        member.permissions().put(permission, value == TriState.TRUE);
        if (member.status().isMember()) {
            data.guildsByPlayer().put(playerId, guild);
        }
        if (level instanceof ServerLevel serverLevel) {
            resolveMemberName(serverLevel, guild.members(), playerId);
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundModifyGuildPermissionPacket(guild.id(), playerId, permission, value), serverLevel.getServer());
        }
        ArgonautsEvents.ModifyGuildMemberEvent.fire(level, guild, playerId, member.status());
    }

    @Override
    public void modifySetting(Level level, Guild guild, Setting<?> setting, String settingId) {
        var data = GuildSaveData.read(level);
        guild.settings().put(settingId, setting);
        if (level instanceof ServerLevel serverLevel) {
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundModifyGuildSettingPacket(guild.id(), setting, settingId), serverLevel.getServer());
        }
        ArgonautsEvents.GuildChangedEvent.fire(level, guild);
    }

    @Override
    public Optional<Guild> get(Level level, UUID id) {
        return Optional.ofNullable(GuildSaveData.read(level).guilds().get(id));
    }

    @Override
    public Optional<Guild> getPlayerGuild(Level level, UUID playerId) {
        return Optional.ofNullable(GuildSaveData.read(level).guildsByPlayer().get(playerId));
    }

    @Override
    public Set<Guild> getAll(Level level) {
        return GuildSaveData.read(level).guilds().values().stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Map<UUID, Guild> getAllGuildsByPlayer(Level level) {
        return GuildSaveData.read(level).guildsByPlayer();
    }

}
