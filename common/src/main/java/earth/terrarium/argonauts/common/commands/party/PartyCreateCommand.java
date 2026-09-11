package earth.terrarium.argonauts.common.commands.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.settings.Settings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static earth.terrarium.argonauts.common.utils.ModUtils.formatTextColors;

public final class PartyCreateCommand {


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("party")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> {
                            create(context.getSource(), formatTextColors(StringArgumentType.getString(context, "name")));
                            return 1;
                        }))
                    .executes(context -> {
                        String name = ModUtils.translatableWithStyle("command.argonauts.party_name", context.getSource().getPlayerOrException().getName()).getString();
                        create(context.getSource(), name);
                        return 1;
                    })
                )
            ));
    }

    private static void create(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (PartyApi.API.getPlayerParty(player).isPresent()) throw TeamExceptions.ALREADY_IN_PARTY.create();
        if (name.length() > Settings.MAX_NAME_LENGTH) throw TeamExceptions.NAME_TOO_LONG.create();
        Party party = new Party(player.getUUID(), name);
        PartyApi.API.create(source.getLevel(), party);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.party_create", party.displayName()), false);
    }
}
