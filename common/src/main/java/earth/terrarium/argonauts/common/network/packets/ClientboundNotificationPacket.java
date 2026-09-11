package earth.terrarium.argonauts.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.NetworkHandle;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.client.NotificationManager;
import net.minecraft.network.chat.Component;

public record ClientboundNotificationPacket(String key) implements Packet<ClientboundNotificationPacket> {

    public static final ClientboundPacketType<ClientboundNotificationPacket> TYPE = CodecPacketType.Client.create(
        Argonauts.id("notification"),
        ObjectByteCodec.create(ByteCodec.STRING.fieldOf(ClientboundNotificationPacket::key), ClientboundNotificationPacket::new),
        NetworkHandle.handle(packet -> NotificationManager.show(Component.translatable(packet.key())))
    );

    @Override
    public PacketType<ClientboundNotificationPacket> type() {
        return TYPE;
    }
}
