package earth.terrarium.argonauts.api.teams.party;

import earth.terrarium.argonauts.api.ApiHelper;
import earth.terrarium.argonauts.api.teams.MemberStatus;
import earth.terrarium.argonauts.api.teams.settings.Setting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PartyApi {

    PartyApi API = ApiHelper.load(PartyApi.class);

    void create(Level level, Party party);

    void disband(Level level, Party party);

    void join(Level level, Party party, UUID playerId);

    void leave(Level level, Party party, UUID playerId);

    void modifyMember(Level level, Party party, UUID playerId, MemberStatus status);

    void modifyPermission(Level level, Party party, UUID playerId, String permission, boolean value);

    void modifySetting(Level level, Party party, Setting<?> setting, String settingId);

    Optional<Party> get(Level level, UUID id);

    Optional<Party> getPlayerParty(Level level, UUID playerId);

    default Optional<Party> getPlayerParty(Player player) {
        return getPlayerParty(player.level(), player.getUUID());
    }

    Set<Party> getAll(Level level);

    Map<UUID, Party> getAllPartiesByPlayer(Level level);

}
