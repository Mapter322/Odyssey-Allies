package earth.terrarium.argonauts.common.commands.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.common.commands.TeamArguments;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class PartyKickCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("party")
                .then(Commands.literal("kick")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(TeamSuggestionProviders.CURRENT_PARTY_MEMBERS_SUGGESTION_PROVIDER)
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
        Party party = PartyApi.API.getPlayerParty(player).orElse(null);
        if (party == null) throw TeamExceptions.NOT_IN_PARTY.create();
        if (!party.canManageMembers(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_MEMBERS.create();
        UUID targetId = TeamArguments.resolveMember(source, party, targetName);
        if (targetId == null || !party.isMember(targetId)) throw TeamExceptions.PLAYER_NOT_IN_PARTY.create();
        if (player.getUUID().equals(targetId)) throw TeamExceptions.CANT_KICK_YOURSELF.create();

        Component name = TeamArguments.memberName(source.getServer(), party, targetId);
        PartyApi.API.leave(source.getLevel(), party, targetId);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.kick", name), false);
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(targetId);
        if (online != null) {
            online.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.party_kicked", player.getName(), party.displayName()), false);
        }
    }
}
