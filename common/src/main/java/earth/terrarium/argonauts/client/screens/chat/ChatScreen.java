package earth.terrarium.argonauts.client.screens.chat;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.screens.chat.embeds.EmbedHandler;
import earth.terrarium.argonauts.client.screens.chat.messages.ChatMessageEntry;
import earth.terrarium.argonauts.client.screens.chat.messages.ChatMessagesList;
import earth.terrarium.argonauts.client.utils.ClientUtils;
import earth.terrarium.argonauts.common.chat.ChatHandler;
import earth.terrarium.argonauts.common.chat.ChatMessage;
import earth.terrarium.argonauts.common.constants.ConstantComponents;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.components.textbox.TextBox;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.UIIcons;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatScreen extends BaseScreen {

    private static final int BASE_WIDTH = 280;
    private static final int BASE_HEIGHT = 200;
    private static final int BANNER_HEIGHT = 16;
    private static final int HEADER_PAD = 4;
    private static final int SIDE_PADDING = 8;
    private static final int PANEL_GAP = 4;
    private static final int INSET_PADDING = 2;
    private static final int MEMBERS_WIDTH = 84;
    private static final int INPUT_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = INPUT_HEIGHT + 6;
    private static final int MEMBER_ROW_HEIGHT = 20;

    private final Team team;
    private final List<GameProfile> onlineMembers = new ArrayList<>();
    private final State<String> input = State.of("");

    private ChatMessagesList messages;
    private TextBox inputBox;

    private int contentTop;
    private int messagesPanelX;
    private int membersPanelX;
    private int messagesWidth;
    private int panelsHeight;
    private int inputX;
    private int inputY;
    private int inputWidth;

    @Nullable
    private String embedUrl;

    public ChatScreen(Component displayName, Team team) {
        super(displayName, BASE_WIDTH, BASE_HEIGHT);
        this.team = team;
        this.onlineMembers.addAll(this.team.onlineMembers(Objects.requireNonNull(Minecraft.getInstance().level))
            .stream()
            .map(Player::getGameProfile)
            .toList());
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

        Component onlineText = Component.translatable("gui.argonauts.online_members", this.onlineMembers.size(), this.team.realMembersCount());
        StringWidget onlineLabel = new StringWidget(onlineText, this.font);
        onlineLabel.setColor(0xFFAAAAAA);
        onlineLabel.setPosition(
            this.leftPos + this.imageWidth - 14 - HEADER_PAD - this.font.width(onlineText),
            this.topPos + (BANNER_HEIGHT - this.font.lineHeight) / 2
        );
        this.addRenderableWidget(onlineLabel);

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

        this.contentTop = this.topPos + BANNER_HEIGHT + 4;
        int contentBottom = this.topPos + this.imageHeight - 6;
        int contentWidth = this.imageWidth - SIDE_PADDING * 2;
        this.messagesWidth = contentWidth - MEMBERS_WIDTH - PANEL_GAP;
        this.panelsHeight = contentBottom - contentTop - FOOTER_HEIGHT - PANEL_GAP;

        this.messagesPanelX = this.leftPos + SIDE_PADDING;
        this.membersPanelX = this.messagesPanelX + this.messagesWidth + PANEL_GAP;

        this.messages = new ChatMessagesList(this.messagesWidth - INSET_PADDING * 2, this.panelsHeight - INSET_PADDING * 2);
        this.messages.setPosition(this.messagesPanelX + INSET_PADDING, contentTop + INSET_PADDING);
        for (ChatMessage message : ChatHandler.getChannel(this.team.id())) {
            this.messages.add(new ChatMessageEntry(this.messages.itemCount(), message));
        }
        this.messages.scrollToBottom();
        this.addRenderableWidget(this.messages);
        this.messages.visitWidgets(this::addWidget);

        ListWidget members = new ListWidget(MEMBERS_WIDTH - INSET_PADDING * 2, this.panelsHeight - INSET_PADDING * 2);
        members.withGap(1);
        members.setPosition(this.membersPanelX + INSET_PADDING, contentTop + INSET_PADDING);
        this.onlineMembers.forEach(profile -> members.add(memberRow(profile, MEMBERS_WIDTH - INSET_PADDING * 2)));
        this.addRenderableWidget(members);
        members.visitWidgets(this::addWidget);

        this.inputY = contentBottom - INPUT_HEIGHT + 2;
        this.inputX = this.leftPos + SIDE_PADDING;
        this.inputWidth = contentWidth - INPUT_HEIGHT - PANEL_GAP;
        this.inputBox = Widgets.textInput(this.input)
            .withPlaceholder(ConstantComponents.CHAT_PLACEHOLDER.getString())
            .withMaxLength(ChatMessage.MAX_MESSAGE_LENGTH);
        this.inputBox.withSize(this.inputWidth, INPUT_HEIGHT);
        this.inputBox.setPosition(this.inputX, this.inputY);
        this.addRenderableWidget(this.inputBox);

        Button send = Widgets.button(button -> {
            button.withSize(INPUT_HEIGHT, INPUT_HEIGHT);
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            button.withRenderer(WidgetRenderers.center(12, 12, WidgetRenderers.icon(UIIcons.MESSAGE_REPLY).withColor(MinecraftColors.WHITE)));
            button.withTooltip(ConstantComponents.SEND_CHAT);
            button.withCallback(this::send);
        });
        send.setPosition(this.inputX + this.inputWidth + PANEL_GAP, this.inputY);
        this.addRenderableWidget(send);
    }

    private Button memberRow(GameProfile profile, int width) {
        return Widgets.button(button -> {
            button.withSize(width, MEMBER_ROW_HEIGHT);
            button.withTexture(UIConstants.LIST_ENTRY);
            button.withTooltip(Component.literal(profile.getName()));
            button.withRenderer((graphics, context, partialTick) -> {
                PlayerFaceRenderer.draw(graphics, skin(profile), context.getX() + 2, context.getY() + 2, 16);
                graphics.drawString(
                    this.font,
                    profile.getName(),
                    context.getX() + 21,
                    context.getY() + (context.getHeight() - this.font.lineHeight) / 2,
                    0xFFFFFF,
                    false
                );
            });
        });
    }

    private static ResourceLocation skin(GameProfile profile) {
        var connection = Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(profile.getId());
        if (info != null) return info.getSkin().texture();
        return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile).texture();
    }

    private void send() {
        String value = this.inputBox.getValue().strip();
        if (value.isEmpty()) return;
        ScreenUtils.sendCommand("argonauts " + this.team.type() + " chat " + value);
        this.inputBox.setValue("");
    }

    public void addMessage(ChatMessage message) {
        if (this.messages == null) return;
        boolean atBottom = this.messages.isAtBottom();
        this.messages.add(new ChatMessageEntry(this.messages.itemCount(), message));
        if (atBottom) this.messages.scrollToBottom();
    }

    public void setEmbedUrl(String url) {
        this.embedUrl = url;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String charCount = ClientUtils.getSmallNumber(ChatMessage.MAX_MESSAGE_LENGTH - this.inputBox.getValue().length());
        graphics.drawString(
            this.font,
            charCount,
            this.inputX - this.leftPos + this.inputWidth - this.font.width(charCount),
            this.inputY - this.topPos - this.font.lineHeight - 4,
            0xFFAAAAAA,
            false
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        this.renderBlurredBackground(partialTick);
        this.renderBg(graphics, partialTick, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(this.leftPos, this.topPos, 0.0F);
            this.renderLabels(graphics, mouseX, mouseY);
        }
        RenderSystem.enableDepthTest();
        if (this.embedUrl != null) {
            EmbedHandler.handle(graphics, this.embedUrl);
            this.embedUrl = null;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        RenderSystem.disableDepthTest();
        graphics.blitSprite(UIConstants.MODAL, x, y, this.imageWidth, this.imageHeight);
        graphics.blitSprite(UIConstants.MODAL_HEADER, x, y, this.imageWidth, BANNER_HEIGHT);
        graphics.blitSprite(UIConstants.MODAL_FOOTER, x, y + this.imageHeight - FOOTER_HEIGHT - 2, this.imageWidth, FOOTER_HEIGHT + 2);
        graphics.blitSprite(UIConstants.MODAL_INSET, this.messagesPanelX, this.contentTop, this.messagesWidth, this.panelsHeight);
        graphics.blitSprite(UIConstants.MODAL_INSET, this.membersPanelX, this.contentTop, MEMBERS_WIDTH, this.panelsHeight);
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
        if (this.getFocused() == this.inputBox && keyCode == InputConstants.KEY_RETURN) {
            this.send();
            return true;
        }
        if (Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static void openGuild() {
        GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).ifPresent(guild ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new ChatScreen(ConstantComponents.GUILD_CHAT_TITLE, guild))
            )
        );
    }

    public static void openParty() {
        PartyApi.API.getPlayerParty(Minecraft.getInstance().player).ifPresent(party ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new ChatScreen(ConstantComponents.PARTY_CHAT_TITLE, party))
            )
        );
    }
}
