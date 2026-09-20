package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.MemberStatus;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.constants.ConstantComponents;
import earth.terrarium.argonauts.common.settings.Settings;
import earth.terrarium.argonauts.common.utils.Config;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GuildInviteCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("invite")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            invite(context.getSource(), EntityArgument.getPlayer(context, "player"));
                            return 1;
                        })
                    )
                )
            ));
    }

    private static void invite(CommandSourceStack source, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        if (player.getUUID().equals(targetPlayer.getUUID())) throw TeamExceptions.CANT_INVITE_YOURSELF.create();
        if (!guild.isPublic() && !guild.canManageMembers(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_MEMBERS.create();
        if (guild.isMember(targetPlayer.getUUID())) throw TeamExceptions.PLAYER_IS_GUILD_MEMBER.create();
        if (guild.realMembersCount() >= Config.getMaxMembers(Settings.LEVEL.get(guild))) throw TeamExceptions.GUILD_FULL.create();

        GuildApi.API.modifyMember(source.getLevel(), guild, targetPlayer.getUUID(), MemberStatus.INVITED);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.invite", targetPlayer.getName()), false);
        targetPlayer.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.guild_invited", player.getName(), guild.displayName()), false);
        targetPlayer.displayClientMessage(Component.empty()
            .append(ConstantComponents.inviteButton(ConstantComponents.ACCEPT,
                ModUtils.translatableWithStyle("command.argonauts.join", guild.displayName()),
                "/argonauts guild join " + player.getGameProfile().getName(), ChatFormatting.GREEN))
            .append(Component.literal("    "))
            .append(ConstantComponents.inviteButton(ConstantComponents.DECLINE,
                ModUtils.translatableWithStyle("command.argonauts.decline", guild.displayName()),
                "/argonauts guild decline " + player.getGameProfile().getName(), ChatFormatting.RED)), false);
    }
}
