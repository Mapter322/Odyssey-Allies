package earth.terrarium.argonauts.common.hud;

import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.common.network.NetworkHandler;
import earth.terrarium.argonauts.common.network.packets.ClientboundSyncPartyStatusPacket;
import earth.terrarium.argonauts.common.network.packets.ClientboundSyncPartyStatusPacket.MemberData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PartyHudSync {

    private static final int SYNC_INTERVAL_TICKS = 20;

    private PartyHudSync() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.tickCount % SYNC_INTERVAL_TICKS == 0) {
                send(player);
            }
        }
    }

    private static void send(ServerPlayer viewer) {
        Party party = PartyApi.API.getPlayerParty(viewer).orElse(null);
        if (party == null || !NetworkHandler.CHANNEL.canSendToPlayer(viewer, ClientboundSyncPartyStatusPacket.TYPE)) {
            return;
        }

        List<MemberData> members = new ArrayList<>();
        for (Map.Entry<UUID, Member> entry : party.members().entrySet()) {
            if (!entry.getValue().status().isMember()) {
                continue;
            }
            ServerPlayer member = viewer.serverLevel().getServer().getPlayerList().getPlayer(entry.getKey());
            members.add(member == null ? offline(entry.getKey()) : online(member, entry.getKey()));
        }
        NetworkHandler.CHANNEL.sendToPlayer(new ClientboundSyncPartyStatusPacket(members), viewer);
    }

    private static MemberData online(ServerPlayer member, UUID id) {
        return new MemberData(
            id,
            true,
            member.getHealth(),
            member.getMaxHealth(),
            member.getFoodData().getFoodLevel(),
            member.level().dimension().location().toString(),
            member.getX(),
            member.getY(),
            member.getZ()
        );
    }

    private static MemberData offline(UUID id) {
        return new MemberData(id, false, 0.0F, 20.0F, 0, "", 0.0, 0.0, 0.0);
    }
}
