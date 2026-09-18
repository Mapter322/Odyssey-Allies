package earth.terrarium.argonauts.common.chat;


import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.common.network.NetworkHandler;
import earth.terrarium.argonauts.common.network.packets.ClientboundSendMessagePacket;
import earth.terrarium.argonauts.common.utils.Config;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChatHandler {

    private static final Map<UUID, Set<ChatMessage>> CHANNELS = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger("Argonauts Teams Chat");

    public static Set<ChatMessage> getChannel(UUID teamId) {
        return CHANNELS.computeIfAbsent(teamId, id -> new LinkedHashSet<>());
    }

    public static void setHistory(UUID teamId, List<ChatMessage> messages) {
        Set<ChatMessage> channel = getChannel(teamId);
        channel.clear();
        channel.addAll(messages);
    }

    public static List<ChatMessage> getHistory(MinecraftServer server, UUID teamId) {
        return List.copyOf(ChatSaveData.read(server).channels().getOrDefault(teamId, List.of()));
    }

    public static void removeChannel(UUID teamId) {
        CHANNELS.remove(teamId);
    }

    public static void clearChannels() {
        CHANNELS.clear();
    }

    public static void sendMessage(Level level, Team team, ChatMessage message) {
        LOGGER.info(String.format("[%s] <%s> %s", team.type().toUpperCase(Locale.ROOT), message.profile().getName(), message.message()));

        if (level.isClientSide()) {
            Set<ChatMessage> messages = getChannel(team.id());
            if (messages.size() >= Config.maxChatHistory) messages.remove(messages.iterator().next());
            messages.add(message);
            return;
        }

        MinecraftServer server = level.getServer();
        if (server != null) {
            ChatSaveData data = ChatSaveData.read(server);
            List<ChatMessage> history = data.channels().computeIfAbsent(team.id(), id -> new ArrayList<>());
            history.add(message);
            if (history.size() > Config.maxChatHistory) {
                history.subList(0, history.size() - Config.maxChatHistory).clear();
            }
            data.setDirty();
        }

        ClientboundSendMessagePacket packet = new ClientboundSendMessagePacket(team.id(), message);
        team.onlineMembers(level).forEach(member -> {
            if (team.isMember(member.getUUID())) {
                Component messageComponent = CommonUtils.serverTranslatable("chat.argonauts.message",
                    team.displayName().plainCopy().withColor(team.color().getValue()),
                    ChatType.bind(ChatType.CHAT, member).name(),
                    message.message()
                );
                member.displayClientMessage(messageComponent, false);
            }

            if (NetworkHandler.CHANNEL.canSendToPlayer(member, packet.type())) {
                NetworkHandler.CHANNEL.sendToPlayer(packet, member);
            }
        });
    }
}
