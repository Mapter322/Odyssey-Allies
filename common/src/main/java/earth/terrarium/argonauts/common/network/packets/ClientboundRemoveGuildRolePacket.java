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
import earth.terrarium.argonauts.client.ArgonautsClient;
import earth.terrarium.argonauts.client.screens.roles.RolesScreen;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public record ClientboundRemoveGuildRolePacket(
    UUID id,
    String roleId
) implements Packet<ClientboundRemoveGuildRolePacket> {

    public static final ClientboundPacketType<ClientboundRemoveGuildRolePacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundRemoveGuildRolePacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType<ClientboundRemoveGuildRolePacket> implements ClientboundPacketType<ClientboundRemoveGuildRolePacket> {

        public Type() {
            super(
                ClientboundRemoveGuildRolePacket.class,
                Argonauts.id("remove_guild_role"),
                ObjectByteCodec.create(
                    ByteCodec.UUID.fieldOf(ClientboundRemoveGuildRolePacket::id),
                    ByteCodec.STRING.fieldOf(ClientboundRemoveGuildRolePacket::roleId),
                    ClientboundRemoveGuildRolePacket::new
                )
            );
        }

        @Override
        public Runnable handle(ClientboundRemoveGuildRolePacket packet) {
            return () -> {
                GuildApi.API.get(ArgonautsClient.level(), packet.id()).ifPresent(guild ->
                    GuildRoleApi.API.removeRole(ArgonautsClient.level(), guild, packet.roleId()));
                if (Minecraft.getInstance().screen instanceof RolesScreen screen) screen.refreshRoles();
            };
        }
    }
}
