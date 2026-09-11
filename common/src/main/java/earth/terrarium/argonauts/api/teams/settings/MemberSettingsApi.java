package earth.terrarium.argonauts.api.teams.settings;

import earth.terrarium.argonauts.api.ApiHelper;
import earth.terrarium.argonauts.api.teams.Team;
import java.util.List;
import java.util.UUID;

public interface MemberSettingsApi {
    MemberSettingsApi API = ApiHelper.load(MemberSettingsApi.class);

    void register(MemberSetting setting);

    void setHandler(MemberSettingsHandler handler);

    List<MemberSetting> getSettings(Team team);

    MemberSettingState getState(Team team, UUID player, String setting);

    void request(Team team, UUID player);

    void setState(Team team, UUID player, String setting, MemberSettingState state);
}
