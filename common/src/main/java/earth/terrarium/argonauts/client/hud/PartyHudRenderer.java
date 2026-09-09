package earth.terrarium.argonauts.client.hud;

import com.mojang.authlib.GameProfile;
import com.mojang.math.Axis;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;

public final class PartyHudRenderer {

    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PANEL_WIDTH = 154;
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

        int y = PANEL_Y;
        for (Map.Entry<UUID, Member> entry : party.members().entrySet()) {
            if (entry.getKey().equals(localPlayer.getUUID()) || !entry.getValue().status().isMember()) {
                continue;
            }
            y += renderMember(graphics, minecraft, entry.getKey(), entry.getValue(), PANEL_X, y) + ENTRY_GAP;
        }
    }

    private static int renderMember(GuiGraphics graphics, Minecraft minecraft, UUID playerId, Member member, int x, int y) {
        Player player = minecraft.level.getPlayerByUUID(playerId);
        PlayerInfo playerInfo = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(playerId);
        boolean online = player != null && playerInfo != null;
        float health = online ? Math.max(0.0F, player.getHealth()) : 0.0F;
        float maxHealth = online ? Math.max(1.0F, player.getMaxHealth()) : 20.0F;
        int hunger = online ? player.getFoodData().getFoodLevel() : 0;
        int heartSlots = online ? Math.max(1, (int) Math.ceil(maxHealth / 2.0F)) : ICONS_PER_ROW;
        int heartRows = Math.max(1, (heartSlots + ICONS_PER_ROW - 1) / ICONS_PER_ROW);
        int heartsY = y + 14;
        int foodY = heartsY + heartRows * 10;
        int entryHeight = foodY - y + ICON_SIZE + 3;

        graphics.fill(x, y, x + PANEL_WIDTH, y + entryHeight, 0xA010141B);
        graphics.fill(x, y, x + 2, y + entryHeight, online ? 0xFF4CAF50 : 0xFF666A73);

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

        String distanceText = online ? Math.round(minecraft.player.distanceTo(player)) + "m" : "";
        int nameWidth = PANEL_WIDTH - 31;
        if (!distanceText.isEmpty()) {
            nameWidth -= minecraft.font.width(" " + distanceText) + 14;
        }
        String displayName = trimName(minecraft, name, nameWidth);
        graphics.drawString(minecraft.font, displayName, x + 27, y + 4, online ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        if (!distanceText.isEmpty()) {
            int distanceRight = x + PANEL_WIDTH - 18;
            graphics.drawString(minecraft.font, distanceText, distanceRight - minecraft.font.width(distanceText), y + 4, 0xFFB7C0C8, false);
            renderDirectionArrow(graphics, x + PANEL_WIDTH - 10, y + 9, minecraft.player, player);
        }

        renderHearts(graphics, x + 27, heartsY, heartSlots, health);
        renderFood(graphics, x + 27, foodY, hunger);
        return entryHeight;
    }

    private static void renderDirectionArrow(GuiGraphics graphics, int centerX, int centerY, LocalPlayer localPlayer, Player target) {
        double dx = target.getX() - localPlayer.getX();
        double dz = target.getZ() - localPlayer.getZ();
        if (dx == 0.0 && dz == 0.0) {
            return;
        }

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float angle = Mth.wrapDegrees(targetYaw - localPlayer.getYRot());

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.fill(-1, -5, 1, 4, 0xFFD9E2E8);
        graphics.fill(-3, -3, 3, 0, 0xFFD9E2E8);
        graphics.fill(-4, -1, 4, 1, 0xFFD9E2E8);
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
