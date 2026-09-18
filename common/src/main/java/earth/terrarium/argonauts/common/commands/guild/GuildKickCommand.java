package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.common.commands.TeamArguments;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class GuildKickCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("kick")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(TeamSuggestionProviders.CURRENT_GUILD_MEMBERS_SUGGESTION_PROVIDER)
                        .executes(context -> {
                            kick(context.getSource(), StringArgumentType.getString(context, "player"));
                            return 1;
                        })
                    )
                )
            ));
    }

    private static void kick(CommandSourceStack source, String targetName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        UUID targetId = TeamArguments.resolveMember(source, guild, targetName);
        if (targetId == null) throw TeamExceptions.PLAYER_NOT_IN_GUILD.create();
        Component name = TeamArguments.memberName(source.getServer(), guild, targetId);
        if (guild.isAllied(targetId)) {
            GuildAllyCommands.remove(source, targetId, name);
            return;
        }
        if (!guild.canManageMembers(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_MEMBERS.create();
        if (!guild.isMember(targetId)) throw TeamExceptions.PLAYER_NOT_IN_GUILD.create();
        if (player.getUUID().equals(targetId)) throw TeamExceptions.CANT_KICK_YOURSELF.create();

        GuildApi.API.leave(source.getLevel(), guild, targetId);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.kick", name), false);
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(targetId);
        if (online != null) {
            online.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.guild_kicked", player.getName(), guild.displayName()), false);
        }
    }
}
