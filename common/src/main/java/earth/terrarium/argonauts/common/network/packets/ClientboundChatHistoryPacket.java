package earth.terrarium.argonauts.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.client.screens.chat.ChatScreen;
import earth.terrarium.argonauts.common.chat.ChatMessage;

import java.util.List;
import java.util.UUID;

public record ClientboundChatHistoryPacket(
    UUID id,
    List<ChatMessage> messages
) implements Packet<ClientboundChatHistoryPacket> {

    public static final ClientboundPacketType<ClientboundChatHistoryPacket> TYPE = CodecPacketType.Client.create(
        Argonauts.id("chat_history"),
        ObjectByteCodec.create(
            ByteCodec.UUID.fieldOf(ClientboundChatHistoryPacket::id),
            ChatMessage.BYTE_CODEC.listOf().fieldOf(ClientboundChatHistoryPacket::messages),
            ClientboundChatHistoryPacket::new
        ),
        NetworkHandle.handle(packet -> ChatScreen.setHistory(packet.id(), packet.messages()))
    );

    @Override
    public PacketType<ClientboundChatHistoryPacket> type() {
        return TYPE;
    }
}
