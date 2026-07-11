package earth.terrarium.odyssey_allies.common.party;


import earth.terrarium.odyssey_allies.api.events.AlliesEvents;
import earth.terrarium.odyssey_allies.api.teams.Member;
import earth.terrarium.odyssey_allies.api.teams.MemberStatus;
import earth.terrarium.odyssey_allies.api.teams.party.Party;
import earth.terrarium.odyssey_allies.api.teams.party.PartyApi;
import earth.terrarium.odyssey_allies.api.teams.settings.Setting;

import earth.terrarium.odyssey_allies.common.network.NetworkHandler;
import earth.terrarium.odyssey_allies.common.network.packets.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public class PartyApiImpl implements PartyApi {

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
    public void create(Level level, Party party) {
        var data = PartySaveData.read(level);
        data.parties().put(party.id(), party);
        party.members().forEach((memberId, member) -> {
            if (member.status().isMember()) {
                data.partiesByPlayer().put(memberId, party);
            }
        });
        if (level instanceof ServerLevel serverLevel) {
            resolveMemberNames(serverLevel, party.members());
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundAddPartyPacket(party), serverLevel.getServer());
        }
        AlliesEvents.CreatePartyEvent.fire(level, party);
    }

    @Override
    public void disband(Level level, Party party) {
        var data = PartySaveData.read(level);
        data.parties().remove(party.id());
        party.members().keySet().forEach(data.partiesByPlayer()::remove);
        if (level instanceof ServerLevel serverLevel) {
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundRemovePartyPacket(party.id()), serverLevel.getServer());
        }
        AlliesEvents.RemovePartyEvent.fire(level, party);
    }

    @Override
    public void join(Level level, Party party, UUID playerId) {
        this.modifyMember(level, party, playerId, MemberStatus.MEMBER);
    }

    @Override
    public void leave(Level level, Party party, UUID playerId) {
        var data = PartySaveData.read(level);
        if (party.members().get(playerId).status().isMember()) {
            data.partiesByPlayer().remove(playerId);
        }
        party.members().remove(playerId);
        if (level instanceof ServerLevel serverLevel) {
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundLeavePartyPacket(party.id(), playerId), serverLevel.getServer());
        }
        AlliesEvents.RemovePartyMemberEvent.fire(level, party, playerId);
    }

    @Override
    public void modifyMember(Level level, Party party, UUID playerId, MemberStatus status) {
        var data = PartySaveData.read(level);
        party.getOrCreateMember(playerId).setStatus(status);
        if (status.isMember()) {
            data.partiesByPlayer().put(playerId, party);
        }
        if (level instanceof ServerLevel serverLevel) {
            resolveMemberName(serverLevel, party.members(), playerId);
            data.setDirty();
            String name = party.members().get(playerId).name();
            NetworkHandler.sendToAllClientPlayers(new ClientboundModifyPartyMemberPacket(party.id(), playerId, status, name), serverLevel.getServer());
        }
        AlliesEvents.ModifyPartyMemberEvent.fire(level, party, playerId, status);
    }

    @Override
    public void modifyPermission(Level level, Party party, UUID playerId, String permission, boolean value) {
        var data = PartySaveData.read(level);
        party.getOrCreateMember(playerId).setPermission(permission, value);
        if (party.members().get(playerId).status().isMember()) {
            data.partiesByPlayer().put(playerId, party);
        }
        if (level instanceof ServerLevel serverLevel) {
            resolveMemberName(serverLevel, party.members(), playerId);
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundModifyPartyPermissionPacket(party.id(), playerId, permission, value), serverLevel.getServer());
        }
        AlliesEvents.ModifyPartyMemberEvent.fire(level, party, playerId, party.getOrCreateMember(playerId).status());
    }

    @Override
    public void modifySetting(Level level, Party party, Setting<?> setting, String settingId) {
        var data = PartySaveData.read(level);
        data.parties().put(party.id(), party);
        party.settings().put(settingId, setting);
        if (level instanceof ServerLevel serverLevel) {
            data.setDirty();
            NetworkHandler.sendToAllClientPlayers(new ClientboundModifyPartySettingPacket(party.id(), setting, settingId), serverLevel.getServer());
        }
        AlliesEvents.PartyChangedEvent.fire(level, party);
    }

    @Override
    public Optional<Party> get(Level level, UUID id) {
        return Optional.ofNullable(PartySaveData.read(level).parties().get(id));
    }

    @Override
    public Optional<Party> getPlayerParty(Level level, UUID playerId) {
        return Optional.ofNullable(PartySaveData.read(level).partiesByPlayer().get(playerId));
    }

    @Override
    public Set<Party> getAll(Level level) {
        return PartySaveData.read(level).parties().values().stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Map<UUID, Party> getAllPartiesByPlayer(Level level) {
        return PartySaveData.read(level).partiesByPlayer();
    }
}
