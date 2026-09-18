package earth.terrarium.argonauts.common.commands.party;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.common.commands.TeamArguments;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import earth.terrarium.argonauts.api.util.ModUtils;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class PartyPermissionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        MemberPermissionsApi.API.getPartyPermissions().forEach((permission, defaultValue) ->
            dispatcher.register(Commands.literal("argonauts")
                .then(Commands.literal("party")
                    .then(Commands.literal("permissions")
                        .then(Commands.literal("set")
                            .then(Commands.literal(permission)
                                .then(Commands.argument("player", StringArgumentType.word())
                                    .suggests(TeamSuggestionProviders.CURRENT_PARTY_MEMBERS_SUGGESTION_PROVIDER)
                                    .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean value = BoolArgumentType.getBool(context, "value");
                                            set(context.getSource(), StringArgumentType.getString(context, "player"), permission, value);
                                            return 1;
                                        })
                                    )
                                )
                            )
                        )
                        .then(Commands.literal("get")
                            .then(Commands.literal(permission)
                                .then(Commands.argument("player", StringArgumentType.word())
                                    .suggests(TeamSuggestionProviders.CURRENT_PARTY_MEMBERS_SUGGESTION_PROVIDER)
                                    .executes(context -> {
                                        get(context.getSource(), StringArgumentType.getString(context, "player"), permission);
                                        return 1;
                                    })
                                )
                            )
                        )
                        .then(Commands.literal("list")
                            .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(TeamSuggestionProviders.CURRENT_PARTY_MEMBERS_SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    list(context.getSource(), StringArgumentType.getString(context, "player"));
                                    return 1;
                                })
                            )
                        )
                    )
                )
        ));
    }

    private static void set(CommandSourceStack source, String targetName, String permission, boolean value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Party party = PartyApi.API.getPlayerParty(player).orElse(null);
        if (party == null) throw TeamExceptions.NOT_IN_PARTY.create();
        if (!party.canManagePermissions(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_PERMISSIONS.create();

        UUID targetId = TeamArguments.resolveMember(source, party, targetName);
        if (targetId == null || !party.isMember(targetId)) throw TeamExceptions.PLAYER_NOT_IN_PARTY.create();

        PartyApi.API.modifyPermission(source.getLevel(), party, targetId, permission, value);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.permissions.set",
            MemberPermissionsApi.API.getPermissionName(permission),
            value,
            TeamArguments.memberName(source.getServer(), party, targetId)
        ), false);
    }

    private static void get(CommandSourceStack source, String targetName, String permission) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Party party = PartyApi.API.getPlayerParty(player).orElse(null);
        if (party == null) throw TeamExceptions.NOT_IN_PARTY.create();

        UUID targetId = TeamArguments.resolveMember(source, party, targetName);
        if (targetId == null || !party.isMember(targetId)) throw TeamExceptions.PLAYER_NOT_IN_PARTY.create();

        boolean value = party.hasPermission(targetId, permission);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.permissions.get",
            MemberPermissionsApi.API.getPermissionName(permission),
            value,
            TeamArguments.memberName(source.getServer(), party, targetId)
        ), false);
    }

    private static void list(CommandSourceStack source, String targetName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Party party = PartyApi.API.getPlayerParty(player).orElse(null);
        if (party == null) throw TeamExceptions.NOT_IN_PARTY.create();

        UUID targetId = TeamArguments.resolveMember(source, party, targetName);
        if (targetId == null || !party.isMember(targetId)) throw TeamExceptions.PLAYER_NOT_IN_PARTY.create();

        Object2BooleanMap<String> permissions = party.getOrCreateMember(targetId).permissions();

        permissions.forEach((permission, value) -> source.sendSuccess(() ->
            ModUtils.translatableWithStyle("command.argonauts.permissions.list",
                MemberPermissionsApi.API.getPermissionName(permission),
                value
            ), false));
    }
}
