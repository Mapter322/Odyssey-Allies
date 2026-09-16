package earth.terrarium.argonauts.common.network.packets;

import com.teamresourceful.bytecodecs.base.ByteCodec;
import com.teamresourceful.bytecodecs.base.object.ObjectByteCodec;
import com.teamresourceful.resourcefullib.common.network.Packet;
import com.teamresourceful.resourcefullib.common.network.base.ClientboundPacketType;
import com.teamresourceful.resourcefullib.common.network.base.PacketType;
import com.teamresourceful.resourcefullib.common.network.defaults.CodecPacketType;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.client.ArgonautsClient;
import earth.terrarium.argonauts.client.screens.roles.RolesScreen;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public record ClientboundModifyGuildRemovedConditionPacket(
    UUID id,
    String role,
    String condition,
    boolean removed
) implements Packet<ClientboundModifyGuildRemovedConditionPacket> {

    public static final ClientboundPacketType<ClientboundModifyGuildRemovedConditionPacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundModifyGuildRemovedConditionPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType<ClientboundModifyGuildRemovedConditionPacket> implements ClientboundPacketType<ClientboundModifyGuildRemovedConditionPacket> {

        public Type() {
            super(
                ClientboundModifyGuildRemovedConditionPacket.class,
                Argonauts.id("modify_guild_removed_condition"),
                ObjectByteCodec.create(
                    ByteCodec.UUID.fieldOf(ClientboundModifyGuildRemovedConditionPacket::id),
                    ByteCodec.STRING.fieldOf(ClientboundModifyGuildRemovedConditionPacket::role),
                    ByteCodec.STRING.fieldOf(ClientboundModifyGuildRemovedConditionPacket::condition),
                    ByteCodec.BOOLEAN.fieldOf(ClientboundModifyGuildRemovedConditionPacket::removed),
                    ClientboundModifyGuildRemovedConditionPacket::new
                )
            );
        }

        @Override
        public Runnable handle(ClientboundModifyGuildRemovedConditionPacket packet) {
            return () -> {
                GuildApi.API.get(ArgonautsClient.level(), packet.id()).ifPresent(guild -> {
                    if (packet.removed()) {
                        GuildApi.API.removeDefaultCondition(ArgonautsClient.level(), guild, packet.role(), packet.condition());
                    } else {
                        GuildApi.API.restoreDefaultCondition(ArgonautsClient.level(), guild, packet.role(), packet.condition());
                    }
                });
                if (Minecraft.getInstance().screen instanceof RolesScreen screen) screen.refreshRoles();
            };
        }
    }
}
