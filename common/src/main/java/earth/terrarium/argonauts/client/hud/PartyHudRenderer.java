package earth.terrarium.argonauts.client.hud;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Window;
import com.mojang.math.Axis;
import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.common.network.packets.ClientboundSyncPartyStatusPacket.MemberData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PartyHudRenderer {

    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PANEL_WIDTH = 122;
    private static final int ENTRY_GAP = 2;
    private static final int FACE_SIZE = 16;
    private static final int ICON_SIZE = 9;
    private static final int ICON_PITCH = 9;
    private static final int ICONS_PER_ROW = 10;

    private static final ResourceLocation HEART_CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation HEART_FULL = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_HALF = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation FOOD_EMPTY = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation FOOD_FULL = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation FOOD_HALF = ResourceLocation.withDefaultNamespace("hud/food_half");
    private static final ResourceLocation ARROW_TEXTURE = Argonauts.id("textures/gui/arrow.png");
    private static final int ARROW_SIZE = 7;

    private static final int OFFLINE_COLOR = 0xFF666A73;
    private static final int UNKNOWN_DIMENSION_COLOR = 0xFFFF6EC7;
    private static final Map<String, Integer> DIMENSION_COLORS = Map.of(
        "minecraft:overworld", 0xFF4CAF50,
        "minecraft:the_nether", 0xFFB02E3E,
        "minecraft:the_end", 0xFF9B59D0
    );

    private static final float SMALL_SCREEN_SCALE = 0.5F;
    private static final int SMALL_SCREEN_WIDTH = 480;
    private static final int SMALL_SCREEN_HEIGHT = 270;

    private PartyHudRenderer() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null || minecraft.level == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        Party party = PartyApi.API.getPlayerParty(localPlayer).orElse(null);
        if (party == null || party.members().isEmpty()) {
            return;
        }

        float scale = hudScale(minecraft);
        if (scale == 1.0F) {
            renderParty(graphics, minecraft, localPlayer, party);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        renderParty(graphics, minecraft, localPlayer, party);
        graphics.pose().popPose();
    }

    private static float hudScale(Minecraft minecraft) {
        Window window = minecraft.getWindow();
        return window.getGuiScaledWidth() < SMALL_SCREEN_WIDTH || window.getGuiScaledHeight() < SMALL_SCREEN_HEIGHT
            ? SMALL_SCREEN_SCALE
            : 1.0F;
    }

    private static void renderParty(GuiGraphics graphics, Minecraft minecraft, LocalPlayer localPlayer, Party party) {
        int y = PANEL_Y;
        for (Map.Entry<UUID, Member> entry : party.members().entrySet()) {
            if (entry.getKey().equals(localPlayer.getUUID()) || !entry.getValue().status().isMember()) {
                continue;
            }
            y += renderMember(graphics, minecraft, entry.getKey(), entry.getValue(), PANEL_X, y) + ENTRY_GAP;
        }
    }

    private static int renderMember(GuiGraphics graphics, Minecraft minecraft, UUID playerId, Member member, int x, int y) {
        LocalPlayer localPlayer = minecraft.player;
        Player player = minecraft.level.getPlayerByUUID(playerId);
        PlayerInfo playerInfo = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(playerId);
        boolean tracked = player != null && playerInfo != null;
        MemberData data = tracked ? null : PartyHudData.get(playerId);
        boolean online = tracked || (data != null && data.online());
        float health = tracked ? Math.max(0.0F, player.getHealth()) : online ? Math.max(0.0F, data.health()) : 0.0F;
        float maxHealth = tracked ? Math.max(1.0F, player.getMaxHealth()) : online ? Math.max(1.0F, data.maxHealth()) : 20.0F;
        int hunger = tracked ? player.getFoodData().getFoodLevel() : online ? data.hunger() : 0;
        int heartSlots = online ? Math.max(1, (int) Math.ceil(maxHealth / 2.0F)) : ICONS_PER_ROW;
        int heartRows = Math.max(1, (heartSlots + ICONS_PER_ROW - 1) / ICONS_PER_ROW);
        int heartsY = y + 14;
        int foodY = heartsY + heartRows * 10;
        int entryHeight = foodY - y + ICON_SIZE + 3;

        String dimension = tracked
            ? player.level().dimension().location().toString()
            : data != null ? data.dimension() : "";

        graphics.fill(x, y, x + PANEL_WIDTH, y + entryHeight, 0xA010141B);
        graphics.fill(x, y, x + 2, y + entryHeight, statusColor(dimension, online));

        String name = member.name().isBlank() ? playerId.toString().substring(0, 8) : member.name();
        ResourceLocation skinTexture = playerInfo != null
            ? playerInfo.getSkin().texture()
            : minecraft.getSkinManager().getInsecureSkin(new GameProfile(playerId, name)).texture();
        if (skinTexture != null) {
            PlayerFaceRenderer.draw(graphics, skinTexture, x + 6, y + 5, FACE_SIZE);
            if (!online) {
                // Mark cached/offline skins as unavailable without replacing the avatar.
                graphics.fill(x + 6, y + 5, x + 6 + FACE_SIZE, y + 5 + FACE_SIZE, 0x88000000);
            }
        } else {
            graphics.fill(x + 6, y + 5, x + 6 + FACE_SIZE, y + 5 + FACE_SIZE, 0xFF30343B);
        }

        double dx = 0.0;
        double dy = 0.0;
        double dz = 0.0;
        boolean navigate = false;
        if (tracked) {
            dx = player.getX() - localPlayer.getX();
            dy = player.getY() - localPlayer.getY();
            dz = player.getZ() - localPlayer.getZ();
            navigate = true;
        } else if (online && data.dimension().equals(localPlayer.level().dimension().location().toString())) {
            dx = data.x() - localPlayer.getX();
            dy = data.y() - localPlayer.getY();
            dz = data.z() - localPlayer.getZ();
            navigate = true;
        }

        String distanceText = navigate ? formatDistance((float) Math.sqrt(dx * dx + dy * dy + dz * dz)) : "";
        int arrowCenterX = 0;
        int distanceRight = 0;
        int nameRight = x + PANEL_WIDTH - 5;
        if (!distanceText.isEmpty()) {
            arrowCenterX = x + PANEL_WIDTH - 10;
            distanceRight = arrowCenterX - 8;
            nameRight = distanceRight - 3 - minecraft.font.width(distanceText);
        }
        String displayName = trimName(minecraft, name, nameRight - (x + 27));
        graphics.drawString(minecraft.font, displayName, x + 27, y + 4, online ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        if (!distanceText.isEmpty()) {
            graphics.drawString(minecraft.font, distanceText, distanceRight - minecraft.font.width(distanceText), y + 4, 0xFFB7C0C8, false);
            renderDirectionArrow(graphics, arrowCenterX, y + 7, localPlayer, dx, dz);
        }

        renderHearts(graphics, x + 27, heartsY, heartSlots, health);
        renderFood(graphics, x + 27, foodY, hunger);
        return entryHeight;
    }

    private static int statusColor(String dimension, boolean online) {
        if (!online) {
            return OFFLINE_COLOR;
        }
        return DIMENSION_COLORS.getOrDefault(dimension, UNKNOWN_DIMENSION_COLOR);
    }

    private static void renderDirectionArrow(GuiGraphics graphics, int centerX, int centerY, LocalPlayer localPlayer, double dx, double dz) {
        if (dx == 0.0 && dz == 0.0) {
            return;
        }

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float angle = Mth.wrapDegrees(targetYaw - localPlayer.getYRot());

        graphics.pose().pushPose();
        graphics.pose().translate(centerX + 0.5F, centerY + 0.5F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.pose().translate(-0.5F, -0.5F, 0.0F);
        graphics.blit(ARROW_TEXTURE, -ARROW_SIZE / 2, -ARROW_SIZE / 2, 0, 0, ARROW_SIZE, ARROW_SIZE, ARROW_SIZE, ARROW_SIZE);
        graphics.pose().popPose();
    }

    private static void renderHearts(GuiGraphics graphics, int x, int y, int heartSlots, float health) {
        int fullHearts = Math.min(heartSlots, Math.max(0, (int) (health / 2.0F)));
        boolean hasHalfHeart = fullHearts < heartSlots && health - fullHearts * 2.0F >= 1.0F;

        for (int slot = 0; slot < heartSlots; slot++) {
            int row = slot / ICONS_PER_ROW;
            int column = slot % ICONS_PER_ROW;
            int iconX = x + column * ICON_PITCH;
            int iconY = y + row * 10;
            graphics.blitSprite(HEART_CONTAINER, iconX, iconY, ICON_SIZE, ICON_SIZE);

            if (slot < fullHearts) {
                graphics.blitSprite(HEART_FULL, iconX, iconY, ICON_SIZE, ICON_SIZE);
            } else if (slot == fullHearts && hasHalfHeart) {
                graphics.blitSprite(HEART_HALF, iconX, iconY, ICON_SIZE, ICON_SIZE);
            }
        }
    }

    private static void renderFood(GuiGraphics graphics, int x, int y, int hunger) {
        int fullFood = Math.max(0, Math.min(ICONS_PER_ROW, hunger / 2));
        boolean hasHalfFood = fullFood < ICONS_PER_ROW && hunger % 2 != 0;

        for (int slot = 0; slot < ICONS_PER_ROW; slot++) {
            int iconX = x + slot * ICON_PITCH;
            graphics.blitSprite(FOOD_EMPTY, iconX, y, ICON_SIZE, ICON_SIZE);

            if (slot < fullFood) {
                graphics.blitSprite(FOOD_FULL, iconX, y, ICON_SIZE, ICON_SIZE);
            } else if (slot == fullFood && hasHalfFood) {
                graphics.blitSprite(FOOD_HALF, iconX, y, ICON_SIZE, ICON_SIZE);
            }
        }
    }

    private static String formatDistance(float distance) {
        int meters = Math.round(distance);
        if (meters < 1000) {
            return meters + "m";
        }
        if (meters < 10000) {
            return String.format(Locale.ROOT, "%.1fkm", meters / 1000.0F);
        }
        return Math.round(meters / 1000.0F) + "km";
    }

    private static String trimName(Minecraft minecraft, String name, int maxWidth) {
        if (minecraft.font.width(name) <= maxWidth) {
            return name;
        }
        while (name.length() > 1 && minecraft.font.width(name + "...") > maxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        return name + "...";
    }

}
