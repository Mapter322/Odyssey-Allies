package earth.terrarium.argonauts.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.client.hud.PartyHudData;

import java.util.List;
import java.util.UUID;

public record ClientboundSyncPartyStatusPacket(
    List<MemberData> members
) implements Packet<ClientboundSyncPartyStatusPacket> {

    public static final ClientboundPacketType<ClientboundSyncPartyStatusPacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundSyncPartyStatusPacket> type() {
        return TYPE;
    }

    public record MemberData(
        UUID id,
        boolean online,
        float health,
        float maxHealth,
        int hunger,
        String dimension,
        double x,
        double y,
        double z
    ) {

        public static final ByteCodec<MemberData> BYTE_CODEC = ObjectByteCodec.create(
            ByteCodec.UUID.fieldOf(MemberData::id),
            ByteCodec.BOOLEAN.fieldOf(MemberData::online),
            ByteCodec.FLOAT.fieldOf(MemberData::health),
            ByteCodec.FLOAT.fieldOf(MemberData::maxHealth),
            ByteCodec.INT.fieldOf(MemberData::hunger),
            ByteCodec.STRING.fieldOf(MemberData::dimension),
            ByteCodec.DOUBLE.fieldOf(MemberData::x),
            ByteCodec.DOUBLE.fieldOf(MemberData::y),
            ByteCodec.DOUBLE.fieldOf(MemberData::z),
            MemberData::new
        );
    }

    private static class Type extends CodecPacketType<ClientboundSyncPartyStatusPacket> implements ClientboundPacketType<ClientboundSyncPartyStatusPacket> {

        public Type() {
            super(
                ClientboundSyncPartyStatusPacket.class,
                Argonauts.id("sync_party_status"),
                ObjectByteCodec.create(
                    MemberData.BYTE_CODEC.listOf().fieldOf(ClientboundSyncPartyStatusPacket::members),
                    ClientboundSyncPartyStatusPacket::new
                )
            );
        }

        @Override
        public Runnable handle(ClientboundSyncPartyStatusPacket packet) {
            return () -> PartyHudData.update(packet.members);
        }
    }
}
