package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.NotificationApi;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.settings.Settings;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class GuildLeaveCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("leave")
                    .executes(context -> leave(context.getSource()) ? 1 : 0)
                )
            ));
    }

    private static boolean leave(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();

        if (guild.isOwner(player.getUUID()) && guild.realMembersCount() > 1) {
            NotificationApi.notify(player, "command.argonauts.exception.guild_not_empty");
            return false;
        }

        GuildApi.API.leave(source.getLevel(), guild, player.getUUID());

        if (Settings.ANNOUNCE_LEAVE.get(guild)) {
            guild.onlineMembers(source.getLevel())
                .stream()
                .filter(member -> !member.getUUID().equals(player.getUUID()))
                .forEach(member -> member.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.left_guild", player.getName()), false));
        }
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.leave_guild", guild.displayName()), false);
        return true;
    }
}
