package earth.terrarium.argonauts.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.base.ServerboundPacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.common.chat.ChatHandler;
import earth.terrarium.argonauts.common.network.NetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public record RequestChatHistoryPacket(UUID id) implements Packet<RequestChatHistoryPacket> {

    public static final ServerboundPacketType<RequestChatHistoryPacket> TYPE = CodecPacketType.Server.create(
        Argonauts.id("request_chat_history"),
        ObjectByteCodec.create(
            ByteCodec.UUID.fieldOf(RequestChatHistoryPacket::id),
            RequestChatHistoryPacket::new
        ),
        NetworkHandle.handle((packet, player) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            ServerLevel level = serverPlayer.serverLevel();
            boolean member = GuildApi.API.get(level, packet.id())
                .map(team -> team.isMember(player.getUUID()) || team.isAllied(player.getUUID()))
                .orElse(false)
                || PartyApi.API.get(level, packet.id())
                .map(team -> team.isMember(player.getUUID()))
                .orElse(false);
            if (!member) return;
            NetworkHandler.CHANNEL.sendToPlayer(
                new ClientboundChatHistoryPacket(packet.id(), ChatHandler.getHistory(serverPlayer.server, packet.id())),
                serverPlayer
            );
        })
    );

    @Override
    public PacketType<RequestChatHistoryPacket> type() {
        return TYPE;
    }
}
