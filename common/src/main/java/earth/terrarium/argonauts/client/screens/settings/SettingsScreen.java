package earth.terrarium.argonauts.client.screens.settings;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.api.teams.settings.Setting;
import earth.terrarium.argonauts.api.teams.settings.TeamSettingsApi;
import earth.terrarium.argonauts.api.teams.settings.types.BooleanSetting;
import earth.terrarium.argonauts.api.teams.settings.types.ColorSettings;
import earth.terrarium.argonauts.api.teams.settings.types.StringSetting;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.argonauts.common.constants.ConstantComponents;
import earth.terrarium.argonauts.common.settings.Settings;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.base.renderer.WidgetRenderer;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.OverlayAlignment;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SettingsScreen extends BaseScreen {

    private static final int BANNER_HEIGHT = 16;
    private static final int FOOTER_TOTAL = 28;
    private static final int SIDE_PADDING = 8;
    private static final int HEADER_PAD = 4;

    private static final int WIDGET_H = 16;
    private static final int TOGGLE_W = 26;
    private static final int TOGGLE_H = 14;
    private static final int TOGGLE_HIT_W = 32;
    private static final int TOGGLE_Y_OFFSET = -2;
    private static final int COLOR_INPUT_W = 66;
    private static final int COLOR_PICKER_S = 16;
    private static final int CAROUSEL_COLOR_W = 84;
    private static final int TEXT_INPUT_W = 116;
    private static final int SAVE_W = 80;
    private static final int SAVE_H = 16;

    private final Team team;
    private final Map<String, Setting<?>> settings;
    private final UUID selfId;

    private final Map<String, State<?>> states = new LinkedHashMap<>();
    private final Map<String, Object> originalValues = new LinkedHashMap<>();

    public SettingsScreen(Component displayName, Team team, Map<String, Setting<?>> settings) {
        super(displayName, 240, 280);
        this.team = team;
        this.settings = settings;
        this.selfId = Minecraft.getInstance().getGameProfile().getId();
    }

    @Override
    protected void init() {
        this.imageWidth = Math.min(240, this.width - 12);
        this.imageHeight = Math.min(280, this.height - 12);
        super.init();

        boolean canEdit = team.canManageSettings(this.selfId);

        if (this.states.isEmpty()) {
            this.settings.forEach((id, setting) -> {
                if (setting.hidden()) return;
                if (setting instanceof ColorSettings) {
                    Color current = TeamSettingsApi.API.<Color>getSetting(this.team, id).get(this.team);
                    this.states.put(id, State.of(current));
                    this.originalValues.put(id, current);
                } else if (setting instanceof BooleanSetting) {
                    Boolean current = TeamSettingsApi.API.<Boolean>getSetting(this.team, id).get(this.team);
                    this.states.put(id, State.of(current));
                    this.originalValues.put(id, current);
                } else {
                    String current = TeamSettingsApi.API.getSetting(this.team, id).toStringCommand();
                    this.states.put(id, State.of(current));
                    this.originalValues.put(id, current);
                }
            });
        }

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
        closeButton.setTooltip(Tooltip.create(ConstantComponents.SETTINGS));
        this.addRenderableWidget(closeButton);

        int listWidth = this.imageWidth - SIDE_PADDING * 2;
        int listY = this.topPos + BANNER_HEIGHT + 4;
        int footerTop = this.topPos + this.imageHeight - FOOTER_TOTAL;
        int listHeight = footerTop - listY - 4;

        ListWidget list = new ListWidget(listWidth, listHeight);
        list.withGap(3);
        list.setPosition(this.leftPos + SIDE_PADDING, listY);

        this.settings.forEach((id, setting) -> {
            if (setting.hidden()) return;

            Component title = Component.translatable("setting.argonauts." + id);
            Component description = Component.translatable("setting.argonauts." + id + ".description");

            LabelledEntry row;
            if (setting instanceof ColorSettings) {
                row = colorRow(id, title, canEdit);
            } else if (setting instanceof BooleanSetting) {
                row = booleanRow(id, title, description, canEdit);
            } else if (setting instanceof StringSetting) {
                row = stringRow(id, title, canEdit);
            } else {
                row = fallbackRow(id, title, canEdit);
            }
            row.setLockedWidth();
            row.setWidth(listWidth);
            row.setDrawDivider(true);
            list.add(row);
        });

        FrameLayout footer = new FrameLayout(listWidth, FOOTER_TOTAL);
        footer.setPosition(this.leftPos + SIDE_PADDING, footerTop);
        footer.addChild(Widgets.button(button -> {
            button.withSize(SAVE_W, SAVE_H);
            button.withRenderer(WidgetRenderers.text(ConstantComponents.SAVE).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            if (!canEdit) button.asDisabled();
            button.withCallback(this::saveAll);
        }), layoutSettings -> {
            layoutSettings.alignHorizontallyRight();
            layoutSettings.alignVerticallyMiddle();
        });
        footer.arrangeElements();
        footer.visitWidgets(this::addRenderableWidget);

        this.addRenderableWidget(list);
        list.visitWidgets(this::addWidget);
    }

    @SuppressWarnings("unchecked")
    private LabelledEntry colorRow(String id, Component title, boolean canEdit) {
        State<Color> state = (State<Color>) this.states.get(id);
        Button picker = Widgets.colorPicker(state, false, button -> {
            button.withSize(COLOR_PICKER_S, WIDGET_H);
            if (!canEdit) button.asDisabled();
        }, overlay -> overlay.withAlignment(OverlayAlignment.BOTTOM_RIGHT));
        var carousel = Widgets.carousel(row -> {
            row.withSize(CAROUSEL_COLOR_W, WIDGET_H);
            row.withContents(layout -> {
                layout.withChild(Widgets.colorInput(state, textBox -> {
                    textBox.withSize(COLOR_INPUT_W, WIDGET_H);
                    if (!canEdit) textBox.asDisabled();
                }));
                layout.withChild(picker);
            });
        });
        return new LabelledEntry(this.font, title, carousel)
            .setEntryYOffset(-2)
            ;
    }

    @SuppressWarnings("unchecked")
    private LabelledEntry booleanRow(String id, Component title, Component description, boolean canEdit) {
        State<Boolean> state = (State<Boolean>) this.states.get(id);

        WidgetRenderer<Button> onRenderer = WidgetRenderers.center(TOGGLE_W, TOGGLE_H, WidgetRenderers.sprite(UIConstants.SWITCH_ON));
        WidgetRenderer<Button> offRenderer = WidgetRenderers.center(TOGGLE_W, TOGGLE_H, WidgetRenderers.sprite(UIConstants.SWITCH));

        Button toggle = Widgets.button(button -> {
            button.withSize(TOGGLE_HIT_W, WIDGET_H);
            button.withTexture(null);
            button.withRenderer((graphics, context, partialTick) ->
                    (state.get() ? onRenderer : offRenderer).render(graphics, context, partialTick)
            );
            button.withCallback(() -> state.set(!state.get()));
            button.withTooltip(description);
            if (!canEdit) button.asDisabled();
        });
        return new LabelledEntry(this.font, title, toggle)
            .setEntryYOffset(TOGGLE_Y_OFFSET)
            ;
    }

    @SuppressWarnings("unchecked")
    private LabelledEntry stringRow(String id, Component title, boolean canEdit) {
        State<String> state = (State<String>) this.states.get(id);
        int maxLength = getMaxLength(id);
        var carousel = Widgets.carousel(row -> {
            row.withSize(TEXT_INPUT_W + 4, WIDGET_H);
            row.withContents(layout -> layout.withChild(Widgets.textInput(state, textBox -> {
                textBox.withSize(TEXT_INPUT_W, WIDGET_H);
                textBox.withMaxLength(maxLength);
                if (!canEdit) textBox.asDisabled();
            })));
        });
        return new LabelledEntry(this.font, title, carousel)
            .setEntryYOffset(-2)
            ;
    }

    @SuppressWarnings("unchecked")
    private LabelledEntry fallbackRow(String id, Component title, boolean canEdit) {
        State<String> state = (State<String>) this.states.get(id);
        int maxLength = getMaxLength(id);
        var carousel = Widgets.carousel(row -> {
            row.withSize(TEXT_INPUT_W + 4, WIDGET_H);
            row.withContents(layout -> layout.withChild(Widgets.textInput(state, textBox -> {
                textBox.withSize(TEXT_INPUT_W, WIDGET_H);
                textBox.withMaxLength(maxLength);
                if (!canEdit) textBox.asDisabled();
            })));
        });
        return new LabelledEntry(this.font, title, carousel)
            .setEntryYOffset(-2)
            ;
    }

    private int getMaxLength(String settingId) {
        if (Settings.DISPLAY_NAME.id().equals(settingId)) return Settings.MAX_NAME_LENGTH;
        if (Settings.MOTD.id().equals(settingId)) return Settings.MAX_MOTD_LENGTH;
        return Short.MAX_VALUE;
    }

    private void saveAll() {
        this.states.forEach((id, state) -> {
            Object original = this.originalValues.get(id);
            Object current = state.get();
            if (original == current) return;
            if (original != null && original.equals(current)) return;

            String commandValue;
            if (current instanceof Color color) {
                commandValue = color.toString();
            } else if (current instanceof Boolean bool) {
                commandValue = String.valueOf(bool);
            } else {
                commandValue = String.valueOf(current);
            }
            ScreenUtils.sendCommand("%s settings %s %s".formatted(this.team.type(), id, commandValue));
        });
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
        graphics.blitSprite(UIConstants.MODAL_FOOTER, x, y + this.imageHeight - FOOTER_TOTAL, this.imageWidth, FOOTER_TOTAL);
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

    public static void openGuild() {
        GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).ifPresent(guild ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new SettingsScreen(ConstantComponents.GUILD_SETTINGS_TITLE, guild, TeamSettingsApi.API.getGuildSettings()))
            )
        );
    }

    public static void openParty() {
        PartyApi.API.getPlayerParty(Minecraft.getInstance().player).ifPresent(party ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new SettingsScreen(ConstantComponents.PARTY_SETTINGS_TITLE, party, TeamSettingsApi.API.getPartySettings()))
            )
        );
    }
}