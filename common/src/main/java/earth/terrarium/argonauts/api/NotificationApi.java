package earth.terrarium.argonauts.api;

import earth.terrarium.argonauts.common.network.NetworkHandler;
import earth.terrarium.argonauts.common.network.packets.ClientboundNotificationPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;

public final class NotificationApi {

    private NotificationApi() {
    }

    /**
     * Sends a notification to a player, displayed as an overlay on top of any open screen.
     *
     * @param player  the player
     * @param message the message; if translatable, the translation key is sent so the client can localize it
     */
    public static void notify(ServerPlayer player, Component message) {
        if (!NetworkHandler.CHANNEL.canSendToPlayer(player, ClientboundNotificationPacket.TYPE)) return;
        String key = message.getContents() instanceof TranslatableContents contents ? contents.getKey() : null;
        if (key == null) return;
        NetworkHandler.CHANNEL.sendToPlayer(new ClientboundNotificationPacket(key), player);
    }

    /**
     * Sends a notification to a player from a translation key.
     *
     * @param player the player
     * @param key    the translation key
     */
    public static void notify(ServerPlayer player, String key) {
        if (!NetworkHandler.CHANNEL.canSendToPlayer(player, ClientboundNotificationPacket.TYPE)) return;
        NetworkHandler.CHANNEL.sendToPlayer(new ClientboundNotificationPacket(key), player);
    }
}
