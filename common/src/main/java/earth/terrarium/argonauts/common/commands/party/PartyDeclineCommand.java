package earth.terrarium.argonauts.common.commands.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class PartyDeclineCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("party")
                .then(Commands.literal("decline")
                    .then(Commands.argument("player", EntityArgument.player())
                        .suggests(TeamSuggestionProviders.PLAYERS_IN_PARTY_SUGGESTION_PROVIDER)
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
        Party party = PartyApi.API.getPlayerParty(targetPlayer).orElse(null);
        if (party == null) throw TeamExceptions.PLAYER_NOT_IN_PARTY.create();
        if (!party.isInvited(player.getUUID())) throw TeamExceptions.NOT_INVITED_TO_PARTY.create();

        PartyApi.API.leave(source.getLevel(), party, player.getUUID());

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.decline_party", party.displayName()), false);
        targetPlayer.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.party_declined", player.getName(), party.displayName()), false);
    }
}
