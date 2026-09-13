package earth.terrarium.argonauts.api.teams.settings;

import net.minecraft.network.chat.Component;

public record MemberSetting(String id, Component name, Component description, String parent) {

    public MemberSetting(String id, Component name, Component description) {
        this(id, name, description, "");
    }

    public boolean hasParent() {
        return !this.parent.isEmpty();
    }
}
