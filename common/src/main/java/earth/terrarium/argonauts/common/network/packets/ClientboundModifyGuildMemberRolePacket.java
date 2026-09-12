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
import earth.terrarium.argonauts.client.screens.members.MembersScreen;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public record ClientboundModifyGuildMemberRolePacket(
    UUID id,
    UUID playerId,
    String roleId
) implements Packet<ClientboundModifyGuildMemberRolePacket> {

    public static final ClientboundPacketType<ClientboundModifyGuildMemberRolePacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundModifyGuildMemberRolePacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType<ClientboundModifyGuildMemberRolePacket> implements ClientboundPacketType<ClientboundModifyGuildMemberRolePacket> {

        public Type() {
            super(
                ClientboundModifyGuildMemberRolePacket.class,
                Argonauts.id("modify_guild_member_role"),
                ObjectByteCodec.create(
                    ByteCodec.UUID.fieldOf(ClientboundModifyGuildMemberRolePacket::id),
                    ByteCodec.UUID.fieldOf(ClientboundModifyGuildMemberRolePacket::playerId),
                    ByteCodec.STRING.fieldOf(ClientboundModifyGuildMemberRolePacket::roleId),
                    ClientboundModifyGuildMemberRolePacket::new
                )
            );
        }

        @Override
        public Runnable handle(ClientboundModifyGuildMemberRolePacket packet) {
            return () -> {
                GuildApi.API.get(ArgonautsClient.level(), packet.id()).ifPresent(guild ->
                    GuildRoleApi.API.modifyMemberRole(ArgonautsClient.level(), guild, packet.playerId(), packet.roleId()));
                if (Minecraft.getInstance().screen instanceof MembersScreen screen) screen.refreshMemberSettings();
            };
        }
    }
}
