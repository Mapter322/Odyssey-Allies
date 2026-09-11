package earth.terrarium.argonauts.common.permissions;

import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.network.chat.Component;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingState;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MemberPermissionsApiImpl implements MemberPermissionsApi, MemberSettingsApi {

    private static final Object2BooleanMap<String> PARTY_PERMISSIONS = new Object2BooleanOpenHashMap<>();
    private static final Object2BooleanMap<String> GUILD_PERMISSIONS = new Object2BooleanOpenHashMap<>();
    private final List<MemberSetting> memberSettings = new ArrayList<>();
    private MemberSettingsHandler memberSettingsHandler = new MemberSettingsHandler() {
        @Override public MemberSettingState getState(Team team, UUID player, String setting) { return MemberSettingState.INHERIT; }
        @Override public void request(Team team, UUID player) {}
        @Override public void setState(Team team, UUID player, String setting, MemberSettingState state) {}
    };

    @Override
    public void register(MemberSetting setting) {
        memberSettings.removeIf(existing -> existing.id().equals(setting.id()));
        memberSettings.add(setting);
    }

    @Override
    public void setHandler(MemberSettingsHandler handler) {
        this.memberSettingsHandler = handler;
    }

    @Override
    public List<MemberSetting> getSettings(Team team) {
        return List.copyOf(memberSettings);
    }

    @Override
    public MemberSettingState getState(Team team, UUID player, String setting) {
        return memberSettingsHandler.getState(team, player, setting);
    }

    @Override
    public void request(Team team, UUID player) {
        memberSettingsHandler.request(team, player);
    }

    @Override
    public void setState(Team team, UUID player, String setting, MemberSettingState state) {
        memberSettingsHandler.setState(team, player, setting, state);
    }

    @Override
    public void registerPartyPermission(String name, boolean defaultValue) {
        PARTY_PERMISSIONS.put(name, defaultValue);
    }

    @Override
    public void registerGuildPermission(String name, boolean defaultValue) {
        GUILD_PERMISSIONS.put(name, defaultValue);
    }

    @Override
    public Object2BooleanMap<String> getPartyPermissions() {
        return PARTY_PERMISSIONS;
    }

    @Override
    public Object2BooleanMap<String> getGuildPermissions() {
        return GUILD_PERMISSIONS;
    }

    @Override
    public Object2BooleanMap<String> createDefaultGuildPermissions() {
        Object2BooleanMap<String> permissions = new Object2BooleanOpenHashMap<>();
        GUILD_PERMISSIONS.forEach((permission, value) -> {
            if (value) permissions.put(permission, true);
        });
        return permissions;
    }

    @Override
    public Object2BooleanMap<String> createDefaultPartyPermissions() {
        Object2BooleanMap<String> permissions = new Object2BooleanOpenHashMap<>();
        PARTY_PERMISSIONS.forEach((permission, value) -> {
            if (value) permissions.put(permission, true);
        });
        return permissions;
    }

    @Override
    public Component getPermissionName(String permission) {
        return CommonUtils.serverTranslatable("permission.argonauts." + permission);
    }
}
