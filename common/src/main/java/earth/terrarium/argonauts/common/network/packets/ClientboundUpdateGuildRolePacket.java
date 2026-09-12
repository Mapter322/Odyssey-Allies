package earth.terrarium.argonauts.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.guild.GuildRoleApi;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.client.ArgonautsClient;

import java.util.UUID;

public record ClientboundUpdateGuildRolePacket(
    UUID id,
    Role role
) implements Packet<ClientboundUpdateGuildRolePacket> {

    public static final ClientboundPacketType<ClientboundUpdateGuildRolePacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundUpdateGuildRolePacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType<ClientboundUpdateGuildRolePacket> implements ClientboundPacketType<ClientboundUpdateGuildRolePacket> {

        public Type() {
            super(
                ClientboundUpdateGuildRolePacket.class,
                Argonauts.id("update_guild_role"),
                ObjectByteCodec.create(
                    ByteCodec.UUID.fieldOf(ClientboundUpdateGuildRolePacket::id),
                    Role.BYTE_CODEC.fieldOf(ClientboundUpdateGuildRolePacket::role),
                    ClientboundUpdateGuildRolePacket::new
                )
            );
        }

        @Override
        public Runnable handle(ClientboundUpdateGuildRolePacket packet) {
            return () -> GuildApi.API.get(ArgonautsClient.level(), packet.id()).ifPresent(guild ->
                GuildRoleApi.API.modifyRole(ArgonautsClient.level(), guild, packet.role()));
        }
    }
}
