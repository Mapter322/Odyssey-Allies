package earth.terrarium.argonauts.common.constants;

import com.teamresourceful.resourcefullib.common.utils.CommonUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

public class ConstantComponents {

    public static final Component CLICK_TO_ACCEPT = CommonUtils.serverTranslatable("command.argonauts.click_to_accept");

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
    public static final Component GUILD_MEMBERS_TITLE = Component.translatable("gui.argonauts.guild_members.title");
    public static final Component GUILD_SETTINGS_TITLE = Component.translatable("gui.argonauts.guild_settings.title");

    public static final Component GUILD_MENU_TITLE = Component.translatable("gui.argonauts.guild_info.title");
    public static final Component PARTY_MENU_TITLE = Component.translatable("gui.argonauts.party_info.title");

    public static final Component CLOSE = Component.translatable("gui.argonauts.close");
    public static final Component LEAVE_GUILD = Component.translatable("gui.argonauts.leave_guild");
    public static final Component DISBAND_GUILD = Component.translatable("gui.argonauts.disband_guild");
    public static final Component LEAVE_PARTY = Component.translatable("gui.argonauts.leave_party");
    public static final Component DISBAND_PARTY = Component.translatable("gui.argonauts.disband_party");

    public static final Component MEMBER_STATUS = Component.translatable("gui.argonauts.member_status");
    public static final Component MEMBER_PERMISSIONS = Component.translatable("gui.argonauts.member_permissions");
    public static final Component MEMBER_ACTIONS = Component.translatable("gui.argonauts.member_actions");
    public static final Component SETTINGS = Component.translatable("gui.argonauts.settings");
    public static final Component SAVE = Component.translatable("gui.argonauts.save");

    public static final Component REMOVE_MEMBER = Component.translatable("gui.argonauts.remove_member");
    public static final Component REMOVE = Component.translatable("gui.argonauts.remove");
    public static final Component SELECT_MEMBER = Component.translatable("gui.argonauts.select_member");

    public static final Component MAX_GUILD_MEMBERS = Component.translatable("gui.argonauts.max_guild_members");
    public static final Component MAX_PARTY_MEMBERS = Component.translatable("gui.argonauts.max_party_members");

    public static final Component INVENTORY_GUILD_BUTTON = Component.translatable("gui.argonauts.inventory.guild_button");
    public static final Component INVENTORY_PARTY_BUTTON = Component.translatable("gui.argonauts.inventory.party_button");
}
