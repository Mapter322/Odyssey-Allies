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
import earth.terrarium.argonauts.client.screens.members.MembersScreen;
import earth.terrarium.argonauts.client.screens.roles.RolesScreen;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public record ClientboundModifyGuildConditionPacket(
    UUID id,
    String role,
    String condition,
    boolean added
) implements Packet<ClientboundModifyGuildConditionPacket> {

    public static final ClientboundPacketType<ClientboundModifyGuildConditionPacket> TYPE = new Type();

    @Override
    public PacketType<ClientboundModifyGuildConditionPacket> type() {
        return TYPE;
    }

    private static class Type extends CodecPacketType<ClientboundModifyGuildConditionPacket> implements ClientboundPacketType<ClientboundModifyGuildConditionPacket> {

        public Type() {
            super(
                ClientboundModifyGuildConditionPacket.class,
                Argonauts.id("modify_guild_condition"),
                ObjectByteCodec.create(
                    ByteCodec.UUID.fieldOf(ClientboundModifyGuildConditionPacket::id),
                    ByteCodec.STRING.fieldOf(ClientboundModifyGuildConditionPacket::role),
                    ByteCodec.STRING.fieldOf(ClientboundModifyGuildConditionPacket::condition),
                    ByteCodec.BOOLEAN.fieldOf(ClientboundModifyGuildConditionPacket::added),
                    ClientboundModifyGuildConditionPacket::new
                )
            );
        }

        @Override
        public Runnable handle(ClientboundModifyGuildConditionPacket packet) {
            return () -> {
                GuildApi.API.get(ArgonautsClient.level(), packet.id()).ifPresent(guild -> {
                    if (packet.added()) {
                        GuildApi.API.addCondition(ArgonautsClient.level(), guild, packet.role(), packet.condition());
                    } else {
                        GuildApi.API.removeCondition(ArgonautsClient.level(), guild, packet.role(), packet.condition());
                    }
                });
                if (Minecraft.getInstance().screen instanceof MembersScreen screen) screen.refreshMemberSettings();
                if (Minecraft.getInstance().screen instanceof RolesScreen screen) screen.refreshRoles();
            };
        }
    }
}
