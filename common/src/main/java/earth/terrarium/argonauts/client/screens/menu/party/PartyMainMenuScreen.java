package earth.terrarium.argonauts.client.screens.menu.party;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.screens.chat.ChatScreen;
import earth.terrarium.argonauts.client.screens.members.MembersScreen;
import earth.terrarium.argonauts.client.screens.settings.SettingsScreen;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.argonauts.common.constants.ConstantComponents;
import earth.terrarium.argonauts.common.settings.Settings;
import earth.terrarium.argonauts.common.utils.Config;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.components.string.TextWidget;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.context.DividerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.UUID;

public class PartyMainMenuScreen extends BaseScreen {

    private static final int BANNER_HEIGHT = 16;
    private static final int SIDE_PADDING = 8;
    private static final int HEADER_PAD = 4;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ITEM_GAP = 3;
    private static final int LABEL_LEFT_PAD = 2;

    private static final int BASE_WIDTH = 140;
    private static final int BASE_HEIGHT = 200;

    private final Team team;
    private final UUID selfId;
    private final boolean isOwner;

    public PartyMainMenuScreen(Team team) {
        super(ConstantComponents.PARTY_MENU_TITLE, BASE_WIDTH, BASE_HEIGHT);
        this.team = team;
        this.selfId = Minecraft.getInstance().getGameProfile().getId();
        this.isOwner = team.isOwner(this.selfId);
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(BASE_WIDTH, this.width - 12);
        this.imageHeight = Math.min(BASE_HEIGHT, this.height - 12);
        super.init();

        StringWidget titleLabel = new StringWidget(this.title, this.font);
        titleLabel.setColor(0xFFFFFF);
        titleLabel.setPosition(
            this.leftPos + HEADER_PAD,
            this.topPos + (BANNER_HEIGHT - this.font.lineHeight) / 2
        );
        this.addRenderableWidget(titleLabel);

        ImageButton closeButton = new ImageButton(
            0, 0, 11, 11,
            UIConstants.MODAL_CLOSE,
            button -> {
                if (this.canGoBack()) {
                    this.goBack();
                } else {
                    this.onClose();
                }
            }
        );
        closeButton.setPosition(
            this.leftPos + this.imageWidth - 11 - HEADER_PAD,
            this.topPos + (BANNER_HEIGHT - 11) / 2
        );
        closeButton.setTooltip(Tooltip.create(ConstantComponents.CLOSE));
        this.addRenderableWidget(closeButton);

        int contentTop = this.topPos + BANNER_HEIGHT + 4;
        int contentBottom = this.topPos + this.imageHeight - 6;
        int contentHeight = contentBottom - contentTop;
        int contentWidth = this.imageWidth - SIDE_PADDING * 2;

        ListWidget list = new ListWidget(contentWidth, contentHeight);
        list.withGap(ITEM_GAP);
        list.setPosition(this.leftPos + SIDE_PADDING, contentTop);
        buildContent(list, contentWidth);
        this.addRenderableWidget(list);
        list.visitWidgets(this::addWidget);
    }

    private void buildContent(ListWidget list, int width) {
        // --- Info section (top) ---
        list.add(nameHeader());

        list.add(new DividerWidget());

        list.add(labelled(Component.translatable("gui.argonauts.info.owner"), ownerName()));
        list.add(labelled(Component.translatable("gui.argonauts.info.members"), membersValue()));
        list.add(labelled(Component.translatable("gui.argonauts.info.online"), onlineValue()));

        // --- Button section (bottom) ---
        list.add(new DividerWidget());

        list.add(navButton(width, Component.translatable("gui.argonauts.info.members_button"), MembersScreen::openParty));
        list.add(navButton(width, Component.translatable("gui.argonauts.info.chat_button"), ChatScreen::openParty));
        list.add(navButton(width, Component.translatable("gui.argonauts.info.settings_button"), SettingsScreen::openParty));

        list.add(new DividerWidget());

        if (this.isOwner) {
            list.add(dangerButton(width, ConstantComponents.DISBAND_PARTY, () -> sendCommand("argonauts party disband")));
        } else {
            list.add(dangerButton(width, ConstantComponents.LEAVE_PARTY, () -> sendCommand("argonauts party leave")));
        }
    }

    private TextWidget nameHeader() {
        Color color = this.team.color();
        return Widgets.text(this.team.displayName())
            .withColor(color)
            .withLeftAlignment()
            .withShadow();
    }

    private LabelledEntry labelled(Component label, Component value) {
        TextWidget valueWidget = Widgets.text(value)
            .withColor(MinecraftColors.WHITE)
            .withRightAlignment();
        LabelledEntry entry = new LabelledEntry(this.font, label, valueWidget);
        entry.setColor(0xFFAAAAAA);
        entry.setLeftPadding(LABEL_LEFT_PAD);
        entry.setDrawDivider(true);
        return entry;
    }

    private Button navButton(int width, Component text, Runnable action) {
        return Widgets.button(button -> {
            button.withSize(width, BUTTON_HEIGHT);
            button.withRenderer(WidgetRenderers.text(text).withColor(MinecraftColors.DARK_GRAY));
            button.withTexture(UIConstants.BUTTON);
            button.withCallback(action);
        });
    }

    private Button dangerButton(int width, Component text, Runnable action) {
        return Widgets.button(button -> {
            button.withSize(width, BUTTON_HEIGHT);
            button.withRenderer(WidgetRenderers.text(text).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.DANGER_BUTTON);
            button.withCallback(action);
        });
    }

    private Component ownerName() {
        UUID ownerId = this.team.getOwner();
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(ownerId);
            if (info != null) return Component.literal(info.getProfile().getName());
        }
        return Component.literal(ownerId.toString().substring(0, 8));
    }

    private Component membersValue() {
        int count = this.team.realMembersCount();
        int max = Config.maxPartyMembers;
        return Component.literal(count + "/" + max);
    }

    private Component onlineValue() {
        int online = this.team.onlineMembers(Objects.requireNonNull(Minecraft.getInstance().level)).size();
        int max = Config.maxPartyMembers;
        return Component.literal(online + "/" + max);
    }

    private void sendCommand(String command) {
        ScreenUtils.sendCommand(command);
        this.onClose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        this.renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        RenderSystem.disableDepthTest();
        graphics.blitSprite(UIConstants.MODAL, x, y, this.imageWidth, this.imageHeight);
        graphics.blitSprite(UIConstants.MODAL_HEADER, x, y, this.imageWidth, BANNER_HEIGHT);
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (var listener : this.children()) {
            if (listener.mouseClicked(mx, my, button)) {
                this.setFocused(listener);
                if (button == 0) this.setDragging(true);
                return true;
            }
        }
        this.setFocused(null);
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static void open() {
        PartyApi.API.getPlayerParty(Minecraft.getInstance().player).ifPresent(party ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new PartyMainMenuScreen(party))
            )
        );
    }
}
