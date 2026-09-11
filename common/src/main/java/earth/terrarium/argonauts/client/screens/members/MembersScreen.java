package earth.terrarium.argonauts.client.screens.members;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import earth.terrarium.argonauts.client.Modals;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingState;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.client.screens.BaseScreen;
import earth.terrarium.argonauts.client.widget.LabelledEntry;
import earth.terrarium.argonauts.common.constants.ConstantComponents;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.ListWidget;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.compound.radio.RadioState;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.components.string.TextWidget;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import earth.terrarium.olympus.client.ui.UIConstants;
import earth.terrarium.olympus.client.ui.context.DividerWidget;
import earth.terrarium.olympus.client.utils.State;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import com.teamresourceful.resourcefullib.common.utils.TriState;

public class MembersScreen extends BaseScreen {

    private static final int BASE_WIDTH = 300;
    private static final int BASE_HEIGHT = 210;

    private static final int BANNER_HEIGHT = 16;
    private static final int SIDE_PADDING = 8;
    private static final int HEADER_PAD = 4;
    private static final int PANEL_GAP = 4;
    private static final int INSET_PADDING = 4;

    private static final int LIST_WIDTH = 90;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int ENTRY_GAP = 3;

    private static final int TOGGLE_W = 26;
    private static final int TOGGLE_H = 14;
    private static final int BUTTON_HEIGHT = 20;

    private final Team team;
    private final Set<String> permissions;
    private final List<MemberEntry> members = new ArrayList<>();
    private final Map<String, RadioState<TriState>> memberSettingStates = new HashMap<>();
    private final Map<String, MemberSettingState> sentMemberSettingStates = new HashMap<>();

    private final UUID selfId;

    @Nullable
    private GameProfile selectedProfile;
    @Nullable
    private Member selectedMember;

    public MembersScreen(Component displayName, Team team, Set<String> permissions) {
        super(displayName, BASE_WIDTH, BASE_HEIGHT);
        this.team = team;
        this.permissions = permissions;
        this.selfId = Minecraft.getInstance().getGameProfile().getId();

        var conn = Minecraft.getInstance().getConnection();
        var skinManager = Minecraft.getInstance().getSkinManager();

        team.members().forEach((uuid, member) -> {
            if (member.status().isFakePlayer()) return;

            PlayerInfo info = conn != null ? conn.getPlayerInfo(uuid) : null;
            if (info != null) {
                this.members.add(new MemberEntry(info.getProfile(), info.getSkin().texture(), true));
            } else {
                String name = !member.name().isEmpty() ? member.name() : uuid.toString().substring(0, 8);
                GameProfile profile = new GameProfile(uuid, name);
                this.members.add(new MemberEntry(profile, skinManager.getInsecureSkin(profile).texture(), false));
            }
        });
    }

    private record MemberEntry(GameProfile profile, ResourceLocation skin, boolean online) {}

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

        Component onlineText = onlineCountText();
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

        int contentTop = this.topPos + BANNER_HEIGHT + 4;
        int contentBottom = this.topPos + this.imageHeight - 6;
        int contentHeight = contentBottom - contentTop;
        int contentWidth = this.imageWidth - SIDE_PADDING * 2;
        int detailsWidth = contentWidth - LIST_WIDTH - PANEL_GAP;

        int listHeight = contentHeight - BUTTON_HEIGHT - ROW_GAP;

        ListWidget memberList = new ListWidget(LIST_WIDTH - 2, listHeight);
        memberList.withGap(ROW_GAP);
        memberList.setPosition(this.leftPos + SIDE_PADDING + 1, contentTop + 1);
        this.members.forEach(entry -> memberList.add(memberRow(entry, LIST_WIDTH - 2)));
        this.addRenderableWidget(memberList);
        memberList.visitWidgets(this::addWidget);

        int inviteY = contentTop + 1 + listHeight + ROW_GAP - 2;
        Button inviteButton = Widgets.button(button -> {
            button.withSize(LIST_WIDTH - 2, BUTTON_HEIGHT);
            button.withTexture(UIConstants.PRIMARY_BUTTON);
            button.withRenderer(WidgetRenderers.text(ConstantComponents.INVITE_MEMBER).withColor(MinecraftColors.WHITE));
            boolean canManage = team.canManageMembers(this.selfId);
            if (!canManage) button.asDisabled();
            button.withCallback(() -> {
                Modals.input(
                    ConstantComponents.INVITE_MEMBER,
                    ConstantComponents.INVITE_MEMBER_DESCRIPTION,
                    ConstantComponents.INVITE_MEMBER_PLACEHOLDER,
                    16,
                    ConstantComponents.INVITE_MEMBER,
                    name -> !name.isBlank(),
                    name -> ScreenUtils.sendCommand("argonauts %s invite %s".formatted(team.type(), name))
                );
            });
        });
        inviteButton.setPosition(this.leftPos + SIDE_PADDING + 1, inviteY);
        this.addRenderableWidget(inviteButton);

        int detailsX = this.leftPos + SIDE_PADDING + LIST_WIDTH + PANEL_GAP + INSET_PADDING;
        int detailsY = contentTop + INSET_PADDING;

        if (this.selectedProfile != null && this.selectedMember != null) {
            ListWidget detailsList = new ListWidget(detailsWidth - INSET_PADDING * 2, contentHeight - INSET_PADDING * 2);
            detailsList.withGap(ENTRY_GAP);
            detailsList.setPosition(detailsX, detailsY);
            buildDetails(detailsList, this.selectedProfile, this.selectedMember, detailsWidth - INSET_PADDING * 2);
            this.addRenderableWidget(detailsList);
            detailsList.visitWidgets(this::addWidget);
        } else {
            TextWidget placeholder = Widgets.text(ConstantComponents.SELECT_MEMBER)
                .withColor(MinecraftColors.GRAY)
                .withLeftAlignment();
            placeholder.setPosition(detailsX, detailsY);
            this.addRenderableWidget(placeholder);
        }
    }

    private Button memberRow(MemberEntry entry, int width) {
        return Widgets.button(button -> {
            button.withSize(width, ROW_HEIGHT);
            button.withTexture(UIConstants.LIST_ENTRY);
            button.withTooltip(Component.literal(entry.profile().getName()));
            button.withRenderer((graphics, context, partialTick) -> {
                int left = context.getX();
                int top = context.getY();

                if (!entry.online()) {
                    graphics.fill(left, top, left + context.getWidth(), top + context.getHeight(), 0x80000000);
                }

                PlayerFaceRenderer.draw(graphics, entry.skin(), left + 2, top + 2, 16);
                graphics.drawString(
                    Minecraft.getInstance().font,
                    entry.profile().getName(),
                    left + 21,
                    top + (context.getHeight() - Minecraft.getInstance().font.lineHeight) / 2,
                    entry.online() ? 0xFFFFFF : 0xAAAAAA,
                    false
                );
            });
            button.withCallback(() -> {
                this.selectedProfile = entry.profile();
                this.selectedMember = this.team.members().get(entry.profile().getId());
                if (this.team.type().equals("guild")) MemberSettingsApi.API.request(this.team, entry.profile().getId());
                rebuildWidgets();
            });
        });
    }

    private void buildDetails(ListWidget list, GameProfile profile, Member member, int width) {
        this.memberSettingStates.clear();
        this.sentMemberSettingStates.clear();
        list.add(statusRow(member));

        list.add(new DividerWidget());
        list.add(section(ConstantComponents.MEMBER_PERMISSIONS));

        boolean canEditPermissions = member.status().isMember() && team.canManagePermissions(this.selfId) && !team.isOwner(profile.getId());
        this.permissions.forEach(permission ->
            list.add(permissionRow(permission, profile, member, canEditPermissions))
        );

        if (this.team.type().equals("guild")) {
            List<MemberSetting> settings = MemberSettingsApi.API.getSettings(this.team);
            if (!settings.isEmpty()) {
                list.add(new DividerWidget());
                list.add(section(Component.translatable("gui.argonauts.member_claim_permissions")));
                settings.forEach(setting -> list.add(memberSettingRow(setting, profile, canEditPermissions)));
            }
        }

        list.add(new DividerWidget());
        list.add(section(ConstantComponents.MEMBER_ACTIONS));
        list.add(removeButton(profile, width));
    }

    private LabelledEntry statusRow(Member member) {
        TextWidget value = Widgets.text(member.status().getDisplayName())
            .withColor(MinecraftColors.WHITE)
            .withRightAlignment();
        LabelledEntry entry = new LabelledEntry(this.font, ConstantComponents.MEMBER_STATUS, value);
        return entry;
    }

    private LabelledEntry permissionRow(String permission, GameProfile profile, Member member, boolean canEdit) {
        Component title = Component.translatable("permission.argonauts." + permission);
        Component description = Component.translatable("permission.argonauts." + permission + ".description");

        State<Boolean> state = new State<>() {
            private boolean value = member.hasPermission(permission);

            @Override
            public void set(Boolean newValue) {
                this.value = newValue;
                ScreenUtils.sendCommand("argonauts %s permissions set %s %s %s".formatted(team.type(), permission, profile.getName(), newValue));
            }

            @Override
            public Boolean get() {
                return this.value;
            }
        };

        Button toggle = Widgets.toggle(state, button -> {
            button.withSize(TOGGLE_W, TOGGLE_H);
            button.withTooltip(description);
            if (!canEdit) button.asDisabled();
        });

        return new LabelledEntry(this.font, title, toggle)
            .setLockedWidth()
            .setEntryYOffset(-2);
    }

    private LabelledEntry memberSettingRow(MemberSetting setting, GameProfile profile, boolean canEdit) {
        MemberSettingState current = MemberSettingsApi.API.getState(this.team, profile.getId(), setting.id());
        TriState value = switch (current) {
            case ALLOW -> TriState.TRUE;
            case DENY -> TriState.FALSE;
            case INHERIT -> TriState.UNDEFINED;
        };
        RadioState<TriState> state = RadioState.of(value, switch (value) {
            case TRUE -> 0;
            case UNDEFINED -> 1;
            case FALSE -> 2;
        });
        this.memberSettingStates.put(setting.id(), state);
        this.sentMemberSettingStates.put(setting.id(), current);
        var toggle = Widgets.tristate(state);
        toggle.withTooltip(setting.description());
        toggle.active = canEdit;
        return new LabelledEntry(this.font, setting.name(), toggle)
            .setLockedWidth()
            .setEntryYOffset(-2);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.selectedProfile == null || !this.team.type().equals("guild")) return;
        this.memberSettingStates.forEach((id, state) -> {
            MemberSettingState value = switch (state.get()) {
                case TRUE -> MemberSettingState.ALLOW;
                case FALSE -> MemberSettingState.DENY;
                case UNDEFINED -> MemberSettingState.INHERIT;
            };
            if (value != this.sentMemberSettingStates.get(id)) {
                MemberSettingsApi.API.setState(this.team, this.selectedProfile.getId(), id, value);
                this.sentMemberSettingStates.put(id, value);
            }
        });
    }

    public void refreshMemberSettings() {
        this.clearWidgets();
        this.init();
    }

    private Button removeButton(GameProfile profile, int width) {
        boolean canManage = team.canManageMembers(this.selfId) && !team.isOwner(profile.getId());
        return Widgets.button(button -> {
            button.withSize(width, BUTTON_HEIGHT);
            button.withRenderer(WidgetRenderers.text(ConstantComponents.REMOVE_MEMBER).withColor(MinecraftColors.WHITE));
            button.withTexture(UIConstants.DANGER_BUTTON);
            if (!canManage) button.asDisabled();
            button.withCallback(() -> {
                ScreenUtils.sendCommand("argonauts %s kick %s".formatted(team.type(), profile.getName()));
                this.selectedProfile = null;
                this.selectedMember = null;
                rebuildWidgets();
            });
        });
    }

    private TextWidget section(Component title) {
        return Widgets.text(title)
            .withColor(MinecraftColors.GOLD)
            .withLeftAlignment();
    }

    private Component onlineCountText() {
        int online = team.onlineMembers(Objects.requireNonNull(Minecraft.getInstance().level)).size();
        int total = team.realMembersCount();
        return Component.translatable("gui.argonauts.online_members", online, total);
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

        // Left panel background (members list)
        int memberListX = x + SIDE_PADDING;
        graphics.blitSprite(UIConstants.MODAL_INSET, memberListX, contentTop, LIST_WIDTH, contentHeight);

        // Right panel background (details)
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
                Minecraft.getInstance().setScreen(new MembersScreen(ConstantComponents.GUILD_MEMBERS_TITLE, guild, MemberPermissionsApi.API.getGuildPermissions().keySet()))
            )
        );
    }

    public static void openParty() {
        PartyApi.API.getPlayerParty(Minecraft.getInstance().player).ifPresent(party ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new MembersScreen(ConstantComponents.PARTY_MEMBERS_TITLE, party, MemberPermissionsApi.API.getPartyPermissions().keySet()))
            )
        );
    }
}
