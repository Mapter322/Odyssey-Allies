package earth.terrarium.argonauts.client.screens.roles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.client.Modals;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.argonauts.client.widget.SettingCategoryEntry;
import earth.terrarium.argonauts.common.commands.TeamArguments;
import earth.terrarium.argonauts.common.constants.ConstantComponents;
import earth.terrarium.argonauts.common.guild.GuildRoleDefaults;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.compound.LayoutWidget;
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.dropdown.DropdownState;
import earth.terrarium.olympus.client.components.renderers.TristateRenderers;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.components.string.TextWidget;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.layouts.LinearViewLayout;
import earth.terrarium.olympus.client.ui.UIConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class RolesScreen extends BaseScreen {

    private static final Pattern ROLE_NAME = Pattern.compile("^[a-z0-9_-]{1,24}$");

    private static final int BASE_WIDTH = 300;
    private static final int BASE_HEIGHT = 210;

    private static final int BANNER_HEIGHT = 16;
    private static final int SIDE_PADDING = 8;
    private static final int HEADER_PAD = 4;
    private static final int PANEL_GAP = 4;
    private static final int INSET_PADDING = 4;

    private static final int LIST_WIDTH = 90;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 0;
    private static final int ENTRY_GAP = 3;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CREATE_LIFT = 2;
    private static final int DROPDOWN_W = 90;
    private static final int DROPDOWN_H = 14;
    private static final int TRISTATE_W = 36;
    private static final int TRISTATE_H = 14;

    private static final Comparator<String> ROLE_ORDER = Comparator
        .comparingInt((String id) -> switch (id) {
            case Role.ALL -> 0;
            case Role.MEMBER -> 1;
            case Role.ALLY -> 2;
            default -> 3;
        })
        .thenComparing(Comparator.naturalOrder());

    private final Guild guild;
    private final UUID selfId;
    private final Set<String> expandedCategories = new HashSet<>();

    @Nullable
    private String selectedRoleId;
    @Nullable
    private DetailsListWidget detailsList;
    @Nullable
    private Integer pendingDetailsScroll;

    public RolesScreen(Component displayName, Guild guild) {
        super(displayName, BASE_WIDTH, BASE_HEIGHT);
        this.guild = guild;
        this.selfId = Minecraft.getInstance().getGameProfile().getId();
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
        int detailsWidth = contentWidth - LIST_WIDTH - PANEL_GAP;

        int listHeight = contentHeight - BUTTON_HEIGHT - ROW_GAP - CREATE_LIFT;

        List<String> roleIds = new ArrayList<>(this.guild.roles().keySet());
        roleIds.sort(ROLE_ORDER);

        ListWidget roleList = new ListWidget(LIST_WIDTH - 2, listHeight);
        roleList.withGap(ROW_GAP);
        roleList.setPosition(this.leftPos + SIDE_PADDING + 1, contentTop + 1);
        roleIds.forEach(roleId -> roleList.add(roleRow(roleId, LIST_WIDTH - 2)));
        this.addRenderableWidget(roleList);
        roleList.visitWidgets(this::addWidget);

        int createY = contentTop + 1 + listHeight + ROW_GAP;
        Button createButton = Widgets.button(button -> {
            button.withSize(LIST_WIDTH - 2, BUTTON_HEIGHT);
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            button.withRenderer(WidgetRenderers.text(ConstantComponents.CREATE_ROLE).withColor(MinecraftColors.WHITE));
            if (!this.guild.canManagePermissions(this.selfId)) button.asDisabled();
            button.withCallback(() -> Modals.input(
                ConstantComponents.CREATE_ROLE,
                ConstantComponents.CREATE_ROLE_DESCRIPTION,
                ConstantComponents.CREATE_ROLE_PLACEHOLDER,
                24,
                ConstantComponents.CREATE_ROLE,
                name -> ROLE_NAME.matcher(name.toLowerCase(Locale.ROOT)).matches() && !name.equalsIgnoreCase("none"),
                name -> ScreenUtils.sendCommand("argonauts guild role create " + name.toLowerCase(Locale.ROOT))
            ));
        });
        createButton.setPosition(this.leftPos + SIDE_PADDING + 1, createY);
        this.addRenderableWidget(createButton);

        int detailsX = this.leftPos + SIDE_PADDING + LIST_WIDTH + PANEL_GAP + INSET_PADDING;
        int detailsY = contentTop + INSET_PADDING;

        Role selected = this.selectedRoleId == null ? null : this.guild.roles().get(this.selectedRoleId);
        if (selected != null) {
            this.detailsList = new DetailsListWidget(detailsWidth - INSET_PADDING * 2, contentHeight - INSET_PADDING * 2);
            this.detailsList.withGap(ENTRY_GAP);
            this.detailsList.setPosition(detailsX, detailsY);
            buildDetails(this.detailsList, selected, detailsWidth - INSET_PADDING * 2);
            if (this.pendingDetailsScroll != null) {
                this.detailsList.restoreScroll(this.pendingDetailsScroll);
                this.pendingDetailsScroll = null;
            }
            this.addRenderableWidget(this.detailsList);
            this.detailsList.visitWidgets(this::addWidget);
        } else {
            this.detailsList = null;
            TextWidget placeholder = Widgets.text(ConstantComponents.SELECT_ROLE)
                .withColor(MinecraftColors.GRAY)
                .withLeftAlignment();
            placeholder.setPosition(detailsX, detailsY);
            this.addRenderableWidget(placeholder);
        }
    }

    private Button roleRow(String roleId, int width) {
        return Widgets.button(button -> {
            button.withSize(width, ROW_HEIGHT);
            button.withTexture(UIConstants.LIST_ENTRY);
            button.withTooltip(roleName(roleId));
            button.withRenderer((graphics, context, partialTick) -> {
                int left = context.getX();
                int top = context.getY();
                boolean selected = roleId.equals(this.selectedRoleId);

                if (selected) {
                    graphics.fill(left, top, left + context.getWidth(), top + context.getHeight(), 0xFF55FFFF);
                    graphics.fill(left + 1, top + 1, left + context.getWidth() - 1, top + context.getHeight() - 1, 0xC0102028);
                }

                graphics.drawString(
                    Minecraft.getInstance().font,
                    roleName(roleId),
                    left + 4,
                    top + (context.getHeight() - Minecraft.getInstance().font.lineHeight) / 2,
                    0xFFFFFF,
                    false
                );
            });
            button.withCallback(() -> {
                this.selectedRoleId = roleId;
                rebuildWidgets();
            });
        });
    }

    private void buildDetails(ListWidget list, Role role, int width) {
        boolean canEdit = this.guild.canManagePermissions(this.selfId);
        list.add(parentRow(role, width));

        if (!role.id().equals(Role.ALL) && !role.id().equals(Role.ALLY)) {
            list.add(section(ConstantComponents.MEMBER_PERMISSIONS));
            List<String> permissions = new ArrayList<>(MemberPermissionsApi.API.getGuildPermissions().keySet());
            permissions.sort(String::compareTo);
            permissions.forEach(permission -> list.add(rolePermissionRow(role, permission, canEdit)));
        }

        List<MemberSetting> settings = MemberSettingsApi.API.getSettings(this.guild);
        if (!settings.isEmpty()) {
            list.add(section(Component.translatable("gui.argonauts.member_claim_permissions")));
            buildSettingRows(list, settings, role, canEdit);
        }

        list.add(section(ConstantComponents.ACTIONS));
        list.add(deleteButton(role, width));
    }

    private LabelledEntry rolePermissionRow(Role role, String permission, boolean canEdit) {
        LayoutWidget<LinearViewLayout> toggle = tristate(
            displayState(role, permission),
            canEdit,
            "argonauts guild role permission " + role.id() + " " + permission
        );
        toggle.withTooltip(Component.translatable("permission.argonauts." + permission + ".description"));
        return new LabelledEntry(this.font, Component.translatable("permission.argonauts." + permission), toggle)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setDrawDivider(true)
            .setDividerYOffset(-1);
    }

    private LabelledEntry roleSettingRow(Role role, MemberSetting setting, boolean canEdit, boolean indented) {
        LayoutWidget<LinearViewLayout> toggle = tristate(
            displayState(role, setting.id()),
            canEdit,
            "argonauts guild role setting " + role.id() + " \"" + setting.id() + "\""
        );
        toggle.withTooltip(setting.description());
        LabelledEntry entry = new LabelledEntry(this.font, setting.name(), toggle)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setDrawDivider(true)
            .setDividerYOffset(-1);
        if (indented) entry.setLeftPadding(14);
        return entry;
    }

    private void buildSettingRows(ListWidget list, List<MemberSetting> settings, Role role, boolean canEdit) {
        Map<String, List<MemberSetting>> children = new LinkedHashMap<>();
        List<MemberSetting> roots = new ArrayList<>();
        for (MemberSetting setting : settings) {
            if (setting.hasParent()) {
                children.computeIfAbsent(setting.parent(), ignored -> new ArrayList<>()).add(setting);
            } else {
                roots.add(setting);
            }
        }
        for (MemberSetting setting : roots) {
            List<MemberSetting> group = children.get(setting.id());
            if (group == null || group.isEmpty()) {
                list.add(roleSettingRow(role, setting, canEdit, false));
            }
        }
        for (MemberSetting setting : roots) {
            List<MemberSetting> group = children.get(setting.id());
            if (group == null || group.isEmpty()) continue;
            list.add(categoryRow(role, setting, canEdit));
            if (!this.expandedCategories.contains(setting.id())) continue;
            group.forEach(child -> list.add(roleSettingRow(role, child, canEdit, true)));
        }
    }

    private SettingCategoryEntry categoryRow(Role role, MemberSetting setting, boolean canEdit) {
        SettingCategoryEntry entry = new SettingCategoryEntry(this.font, setting.name(),
            () -> this.expandedCategories.contains(setting.id()),
            () -> displayState(role, setting.id()),
            () -> toggleCategory(setting.id()),
            value -> ScreenUtils.sendCommand("argonauts guild role setting " + role.id() + " \"" + setting.id() + "\" " + TeamArguments.triStateName(value)),
            canEdit);
        entry.withTooltip(setting.description());
        return entry;
    }

    private void toggleCategory(String key) {
        if (!this.expandedCategories.remove(key)) this.expandedCategories.add(key);
        if (this.detailsList != null) this.pendingDetailsScroll = this.detailsList.getScroll();
        rebuildWidgets();
    }

    private TriState displayState(Role role, String key) {
        TriState state = role.override(key);
        return state == TriState.UNDEFINED && !role.hasParent() ? TriState.FALSE : state;
    }

    private LayoutWidget<LinearViewLayout> tristate(TriState current, boolean canEdit, String command) {
        RadioState<TriState> state = RadioState.of(current, switch (current) {
            case TRUE -> 0;
            case UNDEFINED -> 1;
            case FALSE -> 2;
        });
        LayoutWidget<LinearViewLayout> toggle = Widgets.tristate(state, builder -> builder
            .withRenderer((option, active) -> WidgetRenderers.layered(
                WidgetRenderers.sprite(active ? TristateRenderers.getButtonSprites(option) : UIConstants.BUTTON),
                WidgetRenderers.icon(TristateRenderers.getIcon(option))
                    .withColor(active ? MinecraftColors.WHITE : TristateRenderers.getColor(option))
                    .withPaddingBottom(1)
                    .withCentered(10, 10)
            ))
            .withSize(TRISTATE_W, TRISTATE_H)
            .withCallback(value -> ScreenUtils.sendCommand(command + " " + TeamArguments.triStateName(value))),
            layout -> {});
        toggle.active = canEdit;
        return toggle;
    }

    private LabelledEntry parentRow(Role role, int width) {
        boolean canEdit = this.guild.canManagePermissions(this.selfId) && !role.id().equals(Role.ALL);
        String current = role.hasParent() ? role.parent() : "none";

        List<String> options = new ArrayList<>();
        options.add("none");
        this.guild.roles().keySet().stream()
            .filter(id -> !id.equals(role.id()) && !this.guild.wouldCreateRoleCycle(role.id(), id))
            .sorted(ROLE_ORDER)
            .forEach(options::add);

        DropdownState<String> state = DropdownState.of(current);
        Button dropdown = Widgets.dropdown(
            state,
            options,
            this::parentName,
            button -> {
                button.withSize(DROPDOWN_W, DROPDOWN_H);
                if (!canEdit) button.asDisabled();
            },
            builder -> builder
                .withSize(DROPDOWN_W, 150)
                .withCallback(parent -> ScreenUtils.sendCommand("argonauts guild role parent " + role.id() + " " + parent))
        );

        return new LabelledEntry(this.font, ConstantComponents.PARENT, dropdown)
            .setLockedWidth()
            .setEntryYOffset(-2)
            .setDrawDivider(true)
            .setDividerYOffset(-1);
    }

    private Component parentName(String roleId) {
        return roleId.equals("none") ? Component.translatable("gui.argonauts.none") : roleName(roleId);
    }

    private Button deleteButton(Role role, int width) {
        boolean canDelete = this.guild.canManagePermissions(this.selfId) && !GuildRoleDefaults.isDefaultRole(role.id());
        return Widgets.button(button -> {
            button.withSize(width, BUTTON_HEIGHT);
            button.withRenderer(WidgetRenderers.text(ConstantComponents.DELETE_ROLE).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.DANGER_BUTTON);
            if (!canDelete) button.asDisabled();
            button.withCallback(() -> Modals.confirm(
                ConstantComponents.DELETE_ROLE,
                ConstantComponents.DELETE_ROLE_DESCRIPTION,
                ConstantComponents.DELETE_ROLE,
                () -> {
                    ScreenUtils.sendCommand("argonauts guild role delete " + role.id());
                    this.selectedRoleId = null;
                }
            ));
        });
    }

    public void refreshRoles() {
        if (this.selectedRoleId != null && !this.guild.roles().containsKey(this.selectedRoleId)) {
            this.selectedRoleId = null;
        }
        if (this.detailsList != null) this.pendingDetailsScroll = this.detailsList.getScroll();
        this.clearWidgets();
        this.init();
    }

    private Component roleName(String roleId) {
        return Component.translatableWithFallback("gui.argonauts.role." + roleId, roleId);
    }

    private TextWidget section(Component title) {
        return Widgets.text(title)
            .withColor(MinecraftColors.GOLD)
            .withLeftAlignment();
    }

    private static class DetailsListWidget extends ListWidget {
        DetailsListWidget(int width, int height) {
            super(width, height);
        }

        void restoreScroll(int scroll) {
            this.scroll = Math.max(0, Math.min(scroll, Math.max(0, this.getContentHeight() - this.getHeight())));
        }
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

        int contentTop = y + BANNER_HEIGHT + 4;
        int contentHeight = y + this.imageHeight - 6 - contentTop;
        int contentWidth = this.imageWidth - SIDE_PADDING * 2;

        int listX = x + SIDE_PADDING;
        graphics.blitSprite(UIConstants.MODAL_INSET, listX, contentTop, LIST_WIDTH, contentHeight);

        int detailsX = x + SIDE_PADDING + LIST_WIDTH + PANEL_GAP;
        int detailsWidth = contentWidth - LIST_WIDTH - PANEL_GAP;
        graphics.blitSprite(UIConstants.MODAL_INSET, detailsX, contentTop, detailsWidth, contentHeight);

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
                Minecraft.getInstance().setScreen(new RolesScreen(ConstantComponents.GUILD_ROLES_TITLE, guild))
            )
        );
    }
}
