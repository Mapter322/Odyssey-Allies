package earth.terrarium.argonauts.common.party;

import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.MemberStatus;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.settings.Setting;
import earth.terrarium.argonauts.api.teams.settings.TeamSettingsApi;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PartySaveData extends SaveHandler {

    private static final PartySaveData CLIENT_SIDE = new PartySaveData();
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, Party> partiesByPlayer = new HashMap<>();

    @Override
    public void loadData(CompoundTag tag) {
        CompoundTag partiesTag = tag.getCompound("parties");
        partiesTag.getAllKeys().forEach(id -> {
            CompoundTag partyTag = partiesTag.getCompound(id);
            Map<UUID, Member> members = new HashMap<>();
            Map<String, Setting<?>> settings = new HashMap<>();

            CompoundTag membersTag = partyTag.getCompound("members");
            membersTag.getAllKeys().forEach(memberId -> {
                UUID uuid = UUID.fromString(memberId);
                CompoundTag memberTag = membersTag.getCompound(memberId);
                MemberStatus status = MemberStatus.valueOf(memberTag.getString("status").toUpperCase(Locale.ROOT));
                CompoundTag permissionsTag = memberTag.getCompound("permissions");
                Object2BooleanMap<String> permissions = new Object2BooleanOpenHashMap<>();
                permissionsTag.getAllKeys().forEach(permission -> permissions.put(permission, permissionsTag.getBoolean(permission)));
                Member member = new Member(status, permissions);
                member.setName(memberTag.getString("name"));
                members.put(uuid, member);
            });

            CompoundTag settingsTag = partyTag.getCompound("settings");
            settingsTag.getAllKeys().forEach(settingId -> {
                Setting<?> setting = TeamSettingsApi.API.getDefaultValue(settingId).deserialize(settingsTag);
                settings.put(settingId, setting);
            });

            Party party = new Party(UUID.fromString(id), members, settings);
            this.parties.put(party.id(), party);
            members.forEach((memberId, member) -> {
                if (member.status().isMember()) {
                    this.partiesByPlayer.put(memberId, party);
                }
            });
        });
    }

    @Override
    public void saveData(CompoundTag tag) {
        CompoundTag partiesTag = new CompoundTag();
        this.parties.forEach((id, party) -> {
            CompoundTag partyTag = new CompoundTag();

            CompoundTag membersTag = new CompoundTag();
            party.members().forEach((memberId, member) -> {
                if (!member.status().isInvited()) {
                    CompoundTag memberTag = new CompoundTag();
                    memberTag.putString("status", member.status().name().toLowerCase(Locale.ROOT));
                    memberTag.putString("name", member.name());
                    CompoundTag permissionsTag = new CompoundTag();
                    member.permissions().forEach(permissionsTag::putBoolean);
                    memberTag.put("permissions", permissionsTag);
                    membersTag.put(memberId.toString(), memberTag);
                }
            });
            partyTag.put("members", membersTag);

            CompoundTag settingsTag = new CompoundTag();
            party.settings().forEach((name, setting) -> setting.serialize(settingsTag));
            partyTag.put("settings", settingsTag);

            partiesTag.put(id.toString(), partyTag);
        });
        tag.put("parties", partiesTag);
    }

    public static PartySaveData read(Level level) {
        return read(level, HandlerType.create(CLIENT_SIDE, PartySaveData::new), "argonauts_parties");
    }

    public Map<UUID, Party> parties() {
        return this.parties;
    }

    public Map<UUID, Party> partiesByPlayer() {
        return this.partiesByPlayer;
    }
}
