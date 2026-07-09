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

import java.util.Objects;
import java.util.UUID;

public class TeamInfoScreen extends Screen {

    private static final ResourceLocation CLAIM_MENU = OdysseyAllies.id("textures/gui/claim-menu.png");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 180;
    private static final int CLOSE_X = 8;
    private static final int CLOSE_Y = 6;
    private static final int CLOSE_SIZE = 10;
    private final boolean guild;
    private final Team team;
    private final Component teamName;
    private final String ownerName;
    private final int memberCount;
    private final int maxMembers;

    private int leftPos;
    private int topPos;

    public TeamInfoScreen(String teamType, Team team) {
        super(Component.empty());
        this.team = team;
        this.guild = "guild".equals(teamType);
        this.ownerName = getOwnerName(team.getOwner());
        this.memberCount = team.realMembersCount();

        this.maxMembers = guild
            ? Config.maxGuildMembers
            : Config.maxPartyMembers;

        this.teamName = team.displayName().copy();
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

        Component title = guild
            ? Component.translatable("gui.odyssey_allies.guild_info.title")
            : Component.translatable("gui.odyssey_allies.party_info.title");
        graphics.drawString(font, title, x + 18, y + 6, 0x404040, false);

        int relMx = mouseX - this.leftPos;
        int relMy = mouseY - this.topPos;
        boolean closeHovered = relMx >= CLOSE_X && relMx < CLOSE_X + CLOSE_SIZE
            && relMy >= CLOSE_Y && relMy < CLOSE_Y + CLOSE_SIZE;
        graphics.drawString(font, "\u2715", x + CLOSE_X, y + CLOSE_Y, closeHovered ? 0xFFFFFF : 0xAAAAAA, false);

        // Info panel
        int px = x + 12;
        int py = y + 28;
        int pw = WIDTH - 24;
        int ph = 68;

        graphics.fill(px, py, px + pw, py + ph, 0xC0101010);
        graphics.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xC0252525);

        int tx = px + 6;
        int ty = py + 6;

        int onlineCount = team.onlineMembers(Objects.requireNonNull(Minecraft.getInstance().level)).size();


        String typeLabel = guild ? "Guild:" : "Party:";
        graphics.drawString(font, typeLabel, tx, ty, 0xAAAAAA, false);
        graphics.drawString(font, teamName, tx + font.width(typeLabel) + 4, ty, team.color().getValue(), false);

        graphics.drawString(font, Component.translatable("gui.odyssey_allies.info.owner"), tx, ty + 16, 0xAAAAAA, false);
        graphics.drawString(font, ownerName, tx + font.width(Component.translatable("gui.odyssey_allies.info.owner")) + 4, ty + 16, 0xFFFFFF, false);

        String membersValue = memberCount + "/" + maxMembers;
        graphics.drawString(font, Component.translatable("gui.odyssey_allies.info.members"), tx, ty + 32, 0xAAAAAA, false);
        graphics.drawString(font, membersValue, tx + font.width(Component.translatable("gui.odyssey_allies.info.members")) + 4, ty + 32, 0xFFFFFF, false);

        String onlineValue = onlineCount + "/" + maxMembers;
        graphics.drawString(font, Component.translatable("gui.odyssey_allies.info.online"), tx, ty + 48, 0xAAAAAA, false);
        graphics.drawString(font, onlineValue, tx + font.width(Component.translatable("gui.odyssey_allies.info.online")) + 4, ty + 48, 0xFFFFFF, false);
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
    public boolean mouseClicked(double mx, double my, int button) {
        double relMx = mx - this.leftPos;
        double relMy = my - this.topPos;
        if (relMx >= CLOSE_X && relMx < CLOSE_X + CLOSE_SIZE
            && relMy >= CLOSE_Y && relMy < CLOSE_Y + CLOSE_SIZE) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
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
