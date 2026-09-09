package earth.terrarium.argonauts.client.hud;

import earth.terrarium.argonauts.common.network.packets.ClientboundSyncPartyStatusPacket.MemberData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PartyHudData {

    private static final Map<UUID, MemberData> MEMBERS = new HashMap<>();

    private PartyHudData() {
    }

    public static void update(List<MemberData> members) {
        MEMBERS.clear();
        for (MemberData member : members) {
            MEMBERS.put(member.id(), member);
        }
    }

    public static MemberData get(UUID id) {
        return MEMBERS.get(id);
    }
}
