package earth.terrarium.argonauts.client.hud;

import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;

public final class PartyHudRenderer {

    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PANEL_WIDTH = 154;
    private static final int ENTRY_HEIGHT = 34;
    private static final int ENTRY_GAP = 2;
    private static final int FACE_SIZE = 16;

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
            renderMember(graphics, minecraft, entry.getKey(), entry.getValue(), PANEL_X, y);
            y += ENTRY_HEIGHT + ENTRY_GAP;
        }
    }

    private static void renderMember(GuiGraphics graphics, Minecraft minecraft, UUID playerId, Member member, int x, int y) {
        Player player = minecraft.level.getPlayerByUUID(playerId);
        PlayerInfo playerInfo = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(playerId);
        boolean online = player != null && playerInfo != null;

        graphics.fill(x, y, x + PANEL_WIDTH, y + ENTRY_HEIGHT, 0xA010141B);
        graphics.fill(x, y, x + 2, y + ENTRY_HEIGHT, online ? 0xFF4CAF50 : 0xFF666A73);

        if (playerInfo != null) {
            PlayerFaceRenderer.draw(graphics, playerInfo.getSkin().texture(), x + 6, y + 5, FACE_SIZE);
        } else {
            graphics.fill(x + 6, y + 5, x + 6 + FACE_SIZE, y + 5 + FACE_SIZE, 0xFF30343B);
        }

        String name = member.name().isBlank() ? playerId.toString().substring(0, 8) : member.name();
        String displayName = trimName(minecraft, name, PANEL_WIDTH - 31);
        graphics.drawString(minecraft.font, displayName, x + 27, y + 4, online ? 0xFFFFFFFF : 0xFFAAAAAA, false);

        float health = online ? player.getHealth() : 0.0F;
        float maxHealth = online ? player.getMaxHealth() : 1.0F;
        int hunger = online ? player.getFoodData().getFoodLevel() : 0;
        renderBar(graphics, x + 27, y + 16, PANEL_WIDTH - 35, health / maxHealth, 0xFFE55252);
        renderBar(graphics, x + 27, y + 25, PANEL_WIDTH - 35, hunger / 20.0F, 0xFFE4A83A);

        String healthText = online ? formatValue(health) + "/" + formatValue(maxHealth) : "--/--";
        String hungerText = online ? hunger + "/20" : "--/20";
        graphics.drawString(minecraft.font, healthText, x + 30, y + 15, 0xFFFFFFFF, false);
        graphics.drawString(minecraft.font, hungerText, x + 30, y + 24, 0xFFFFFFFF, false);
    }

    private static void renderBar(GuiGraphics graphics, int x, int y, int width, float progress, int color) {
        graphics.fill(x, y, x + width, y + 7, 0xFF292D35);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 7, color);
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

    private static String formatValue(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
