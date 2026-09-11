package earth.terrarium.argonauts.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

//Global client-side notification overlay.

public final class NotificationManager {

    private static final long NOTIFICATION_DURATION = 4000;
    private static final int MAX_NOTIFICATIONS = 4;

    private static final int BACKGROUND = 0xF0111111;
    private static final int BORDER = 0xFF555555;
    private static final int TEXT = 0xFFFF5555;
    private static final int PADDING = 5;
    private static final int TOP_MARGIN = 50;

    private static final List<Notification> notifications = new ArrayList<>();

    private NotificationManager() {
    }

    public static void show(Component message) {
        prune();
        notifications.removeIf(notification -> notification.message().getString().equals(message.getString()));
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.removeFirst();
        }
        notifications.add(new Notification(message, System.currentTimeMillis() + NOTIFICATION_DURATION));
    }

    public static void clear() {
        notifications.clear();
    }

    public static void render(GuiGraphics graphics) {
        prune();
        if (notifications.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int width = notifications.stream()
            .mapToInt(notification -> font.width(notification.message()))
            .max().orElse(0) + PADDING * 2;
        int itemHeight = font.lineHeight + 4;
        int contentHeight = notifications.size() * itemHeight;
        int x = (screenWidth - width) / 2;
        int y = TOP_MARGIN;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 250);
        graphics.fill(x - 1, y - 1, x + width + 1, y + contentHeight + 1, BORDER);
        graphics.fill(x, y, x + width, y + contentHeight, BACKGROUND);
        graphics.enableScissor(x, y, x + width, y + contentHeight);
        for (int i = 0; i < notifications.size(); i++) {
            Component message = notifications.get(i).message();
            graphics.drawString(font, message, x + PADDING, y + i * itemHeight + 2, TEXT, false);
        }
        graphics.disableScissor();
        graphics.pose().popPose();
    }

    private static void prune() {
        notifications.removeIf(notification -> System.currentTimeMillis() > notification.expireAt());
    }

    private record Notification(Component message, long expireAt) {}
}
