package earth.terrarium.argonauts.api.teams.settings;

import net.minecraft.network.chat.Component;

public record MemberSetting(String id, Component name, Component description) {
}
