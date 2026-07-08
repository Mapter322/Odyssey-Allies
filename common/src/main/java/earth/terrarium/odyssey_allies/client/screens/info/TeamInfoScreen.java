package earth.terrarium.odyssey_allies.client.screens.info;

import earth.terrarium.odyssey_allies.OdysseyAllies;
import earth.terrarium.odyssey_allies.api.teams.Team;
import earth.terrarium.odyssey_allies.client.screens.chat.ChatScreen;
import earth.terrarium.odyssey_allies.common.utils.Config;
import earth.terrarium.odyssey_allies.client.screens.members.MembersScreen;
import earth.terrarium.odyssey_allies.client.screens.settings.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class TeamInfoScreen extends Screen {

    private static final ResourceLocation CLAIM_MENU = OdysseyAllies.id("textures/gui/claim-menu.png");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 180;
    private final boolean guild;
    private final Component teamName;
    private final String ownerName;
    private final int memberCount;
    private final int maxMembers;

    private int leftPos;
    private int topPos;

    public TeamInfoScreen(String teamType, Team team) {
        super(Component.empty());
        this.guild = "guild".equals(teamType);
        this.ownerName = getOwnerName(team.getOwner());
        this.memberCount = team.realMembersCount();

        this.maxMembers = guild
            ? Config.maxGuildMembers
            : Config.maxPartyMembers;

        // Color: guild = gold, party = green
        int color = guild ? 0xFFAA00 : 0x55FF55;
        this.teamName = team.displayName().copy().withStyle(style -> style.withColor(color));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - HEIGHT) / 2;

        int btnW = 52;
        int btnH = 20;
        int gap = 4;
        int btnY = this.topPos + 143;
        int startX = this.leftPos + (WIDTH - (3 * btnW + 2 * gap)) / 2;

        addButton(startX, btnY, btnW, btnH,
            Component.translatable("gui.odyssey_allies.info.members_button"),
            () -> { if (guild) MembersScreen.openGuild(); else MembersScreen.openParty(); });

        addButton(startX + btnW + gap, btnY, btnW, btnH,
            Component.translatable("gui.odyssey_allies.info.chat_button"),
            () -> { if (guild) ChatScreen.openGuild(); else ChatScreen.openParty(); });

        addButton(startX + 2 * (btnW + gap), btnY, btnW, btnH,
            Component.translatable("gui.odyssey_allies.info.settings_button"),
            () -> { if (guild) SettingsScreen.openGuild(); else SettingsScreen.openParty(); });
    }

    private void addButton(int x, int y, int w, int h, Component text, Runnable action) {
        this.addRenderableWidget(Button.builder(text, btn -> action.run()).bounds(x, y, w, h).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(CLAIM_MENU, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT, 180, 180);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;

        // Title
        Component title = guild
            ? Component.translatable("gui.odyssey_allies.guild_info.title")
            : Component.translatable("gui.odyssey_allies.party_info.title");
        graphics.drawCenteredString(font, title, x + WIDTH / 2, y + 8, 0xFFFFFF);

        // Info panel
        int px = x + 12;
        int py = y + 28;
        int pw = WIDTH - 24;
        int ph = 50;

        graphics.fill(px, py, px + pw, py + ph, 0xC0101010);
        graphics.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xC0252525);

        int tx = px + 6;
        int ty = py + 6;

        graphics.drawString(font, teamName, tx, ty, 0xFFFFFF, false);
        graphics.drawString(font,
            Component.translatable("gui.odyssey_allies.info.owner", ownerName),
            tx, ty + 16, 0xA0A0A0, false);
        graphics.drawString(font,
            Component.translatable("gui.odyssey_allies.info.members", memberCount, maxMembers),
            tx, ty + 32, 0xA0A0A0, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String getOwnerName(UUID ownerId) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(ownerId);
            if (info != null) return info.getProfile().getName();
        }
        return ownerId.toString().substring(0, 8);
    }
}
