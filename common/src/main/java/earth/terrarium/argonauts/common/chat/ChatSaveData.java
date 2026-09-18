package earth.terrarium.argonauts.common.chat;

import com.mojang.authlib.GameProfile;
import com.teamresourceful.resourcefullib.common.utils.SaveHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatSaveData extends SaveHandler {

    private final Map<UUID, List<ChatMessage>> channels = new HashMap<>();

    @Override
    public void loadData(CompoundTag tag) {
        CompoundTag channelsTag = tag.getCompound("channels");
        channelsTag.getAllKeys().forEach(key -> {
            UUID teamId;
            try {
                teamId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                return;
            }
            List<ChatMessage> messages = new ArrayList<>();
            channelsTag.getList(key, Tag.TAG_COMPOUND).forEach(entry -> {
                CompoundTag messageTag = (CompoundTag) entry;
                messages.add(new ChatMessage(
                    new GameProfile(messageTag.getUUID("uuid"), messageTag.getString("name")),
                    messageTag.getString("message"),
                    Instant.ofEpochMilli(messageTag.getLong("timestamp"))
                ));
            });
            if (!messages.isEmpty()) channels.put(teamId, messages);
        });
    }

    @Override
    public void saveData(CompoundTag tag) {
        CompoundTag channelsTag = new CompoundTag();
        channels.forEach((teamId, messages) -> {
            ListTag list = new ListTag();
            for (ChatMessage message : messages) {
                CompoundTag messageTag = new CompoundTag();
                messageTag.putUUID("uuid", message.profile().getId());
                messageTag.putString("name", message.profile().getName());
                messageTag.putString("message", message.message());
                messageTag.putLong("timestamp", message.timestamp().toEpochMilli());
                list.add(messageTag);
            }
            channelsTag.put(teamId.toString(), list);
        });
        tag.put("channels", channelsTag);
    }

    public Map<UUID, List<ChatMessage>> channels() {
        return this.channels;
    }

    public static ChatSaveData read(MinecraftServer server) {
        return read(server.overworld().getDataStorage(), HandlerType.create(ChatSaveData::new), "argonauts_chat");
    }
}
