package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class GuildDeclineCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("decline")
                    .then(Commands.argument("player", EntityArgument.player())
                        .suggests(TeamSuggestionProviders.PLAYERS_IN_GUILD_SUGGESTION_PROVIDER)
                        .executes(context -> {
                            decline(context.getSource(), EntityArgument.getPlayer(context, "player"));
                            return 1;
                        })
                    )
                )
            ));
    }

    private static void decline(CommandSourceStack source, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(targetPlayer).orElse(null);
        if (guild == null) throw TeamExceptions.PLAYER_NOT_IN_GUILD.create();
        if (!guild.isInvited(player.getUUID())) throw TeamExceptions.NOT_INVITED_TO_GUILD.create();

        GuildApi.API.leave(source.getLevel(), guild, player.getUUID());

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.decline_guild", guild.displayName()), false);
        targetPlayer.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.guild_declined", player.getName(), guild.displayName()), false);
    }
}
