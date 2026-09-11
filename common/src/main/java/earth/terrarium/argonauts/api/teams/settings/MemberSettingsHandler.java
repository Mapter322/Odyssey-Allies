package earth.terrarium.argonauts.api.teams.settings;

import earth.terrarium.argonauts.api.teams.Team;
import java.util.UUID;

public interface MemberSettingsHandler {
    MemberSettingState getState(Team team, UUID player, String setting);

    void request(Team team, UUID player);

    void setState(Team team, UUID player, String setting, MemberSettingState state);
}
