package earth.terrarium.argonauts.common.constants;

import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public class ConstantComponents {

    public static final Component ACCEPT = CommonUtils.serverTranslatable("command.argonauts.accept_button");
    public static final Component DECLINE = CommonUtils.serverTranslatable("command.argonauts.decline_button");

    public static final Component MOTD = CommonUtils.serverTranslatable("motd.argonauts.title");
    public static final Component MOTD_HEADER = CommonUtils.serverTranslatable("motd.argonauts.header").copy().setStyle(Style.EMPTY
        .withColor(ChatFormatting.GRAY)
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, MOTD)));
    public static final Component MOTD_LINE = CommonUtils.serverTranslatable("motd.argonauts.line").copy().setStyle(Style.EMPTY
        .withStrikethrough(true)
        .withColor(ChatFormatting.GRAY));

    public static final Component ODYSSEY_CATEGORY = Component.translatable("key.categories.project_odyssey");
    public static final Component KEY_OPEN_PARTY_CHAT = Component.translatable("key.argonauts.open_party_chat");
    public static final Component KEY_OPEN_GUILD_CHAT = Component.translatable("key.argonauts.open_guild_chat");

    public static final Component PARTY_CHAT_TITLE = Component.translatable("gui.argonauts.party_chat.title");
    public static final Component PARTY_MEMBERS_TITLE = Component.translatable("gui.argonauts.party_members.title");
    public static final Component PARTY_SETTINGS_TITLE = Component.translatable("gui.argonauts.party_settings.title");

    public static final Component GUILD_CHAT_TITLE = Component.translatable("gui.argonauts.guild_chat.title");
    public static final Component CHAT_PLACEHOLDER = Component.translatable("gui.argonauts.chat.placeholder");
    public static final Component SEND_CHAT = Component.translatable("gui.argonauts.chat.send");
    public static final Component GUILD_MEMBERS_TITLE = Component.translatable("gui.argonauts.guild_members.title");
    public static final Component GUILD_ROLES_TITLE = Component.translatable("gui.argonauts.guild_roles.title");
    public static final Component GUILD_SETTINGS_TITLE = Component.translatable("gui.argonauts.guild_settings.title");

    public static final Component GUILD_MENU_TITLE = Component.translatable("gui.argonauts.guild_info.title");
    public static final Component PARTY_MENU_TITLE = Component.translatable("gui.argonauts.party_info.title");

    public static final Component CLOSE = Component.translatable("gui.argonauts.close");
    public static final Component CREATE_GUILD = Component.translatable("gui.argonauts.create_guild");
    public static final Component CREATE_GUILD_DESCRIPTION = Component.translatable("gui.argonauts.create_guild.description");
    public static final Component CREATE_GUILD_PLACEHOLDER = Component.translatable("gui.argonauts.create_guild.placeholder");
    public static final Component CREATE_PARTY = Component.translatable("gui.argonauts.create_party");
    public static final Component CREATE_PARTY_DESCRIPTION = Component.translatable("gui.argonauts.create_party.description");
    public static final Component LEAVE_GUILD = Component.translatable("gui.argonauts.leave_guild");
    public static final Component DISBAND_GUILD = Component.translatable("gui.argonauts.disband_guild");
    public static final Component DISBAND_GUILD_DESCRIPTION = Component.translatable("gui.argonauts.disband_guild.description");
    public static final Component LEAVE_PARTY = Component.translatable("gui.argonauts.leave_party");
    public static final Component DISBAND_PARTY = Component.translatable("gui.argonauts.disband_party");
    public static final Component DISBAND_PARTY_DESCRIPTION = Component.translatable("gui.argonauts.disband_party.description");

    public static final Component ROLE = Component.translatable("gui.argonauts.role");
    public static final Component PARENT = Component.translatable("gui.argonauts.parent");
    public static final Component ACTIONS = Component.translatable("gui.argonauts.actions");
    public static final Component SELECT_ROLE = Component.translatable("gui.argonauts.select_role");
    public static final Component CREATE_ROLE = Component.translatable("gui.argonauts.create_role");
    public static final Component CREATE_ROLE_DESCRIPTION = Component.translatable("gui.argonauts.create_role.description");
    public static final Component CREATE_ROLE_PLACEHOLDER = Component.translatable("gui.argonauts.create_role.placeholder");
    public static final Component DELETE_ROLE = Component.translatable("gui.argonauts.delete_role");
    public static final Component DELETE_ROLE_DESCRIPTION = Component.translatable("gui.argonauts.delete_role.description");
    public static final Component MEMBER_PERMISSIONS = Component.translatable("gui.argonauts.member_permissions");
    public static final Component MEMBER_ACTIONS = Component.translatable("gui.argonauts.member_actions");
    public static final Component ADD_CONDITION = Component.translatable("gui.argonauts.condition.add");
    public static final Component ADD_CONDITION_DESCRIPTION = Component.translatable("gui.argonauts.condition.add.description");
    public static final Component ADD_CONDITION_PLACEHOLDER = Component.translatable("gui.argonauts.condition.add.placeholder");
    public static final Component SETTINGS = Component.translatable("gui.argonauts.settings");
    public static final Component SAVE = Component.translatable("gui.argonauts.save");

    public static final Component REMOVE_MEMBER = Component.translatable("gui.argonauts.remove_member");
    public static final Component CANCEL_INVITE = Component.translatable("gui.argonauts.cancel_invite");
    public static final Component REMOVE = Component.translatable("gui.argonauts.remove");
    public static final Component SELECT_MEMBER = Component.translatable("gui.argonauts.select_member");
    public static final Component INVITE_MEMBER = Component.translatable("gui.argonauts.invite_member");
    public static final Component INVITE_MEMBER_DESCRIPTION = Component.translatable("gui.argonauts.invite_member.description");
    public static final Component INVITE_MEMBER_PLACEHOLDER = Component.translatable("gui.argonauts.invite_member.placeholder");

    public static final Component INVENTORY_GUILD_BUTTON = Component.translatable("gui.argonauts.inventory.guild_button");
    public static final Component INVENTORY_PARTY_BUTTON = Component.translatable("gui.argonauts.inventory.party_button");
    public static final Component INVENTORY_CLAIM_BUTTON = Component.translatable("gui.argonauts.inventory.claim_button");

    public static Component inviteButton(Component label, Component hover, String command, ChatFormatting color) {
        Style style = Style.EMPTY
            .withColor(ChatFormatting.DARK_GRAY)
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        return Component.literal("[ ").withStyle(style)
            .append(label.copy().withStyle(Style.EMPTY.withColor(color)))
            .append(Component.literal(" ]"));
    }
}
