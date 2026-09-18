package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.argonauts.api.teams.MemberStatus;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.common.commands.TeamArguments;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;

import java.util.List;
import java.util.UUID;

public final class GuildAllyCommands {

    public static final SuggestionProvider<CommandSourceStack> CURRENT_GUILD_ALLIES_SUGGESTION_PROVIDER = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        GameProfileCache cache = context.getSource().getServer().getProfileCache();
        List<String> names = guild.members().entrySet().stream()
            .filter(entry -> entry.getValue().status() == MemberStatus.ALLIED)
            .map(entry -> {
                String name = entry.getValue().name();
                if (name.isEmpty() && cache != null) {
                    name = cache.get(entry.getKey()).map(GameProfile::getName).orElse("");
                }
                return name;
            })
            .filter(name -> !name.isEmpty())
            .distinct()
            .toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("allies")
                    .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> {
                                add(context.getSource(), EntityArgument.getPlayer(context, "player"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(CURRENT_GUILD_ALLIES_SUGGESTION_PROVIDER)
                            .executes(context -> {
                                remove(context.getSource(), StringArgumentType.getString(context, "player"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("list")
                        .executes(context -> {
                            list(context.getSource());
                            return 1;
                        })
                    )
                    .executes(context -> {
                        list(context.getSource());
                        return 1;
                    })
                )
            ));
    }

    private static void add(CommandSourceStack source, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        if (!guild.canManageMembers(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_MEMBERS.create();
        if (player.getUUID().equals(targetPlayer.getUUID())) throw TeamExceptions.CANT_ALLY_YOURSELF.create();
        if (guild.isAllied(targetPlayer.getUUID())) throw TeamExceptions.PLAYER_ALREADY_ALLY.create();
        if (guild.isMember(targetPlayer.getUUID())) throw TeamExceptions.PLAYER_IS_GUILD_MEMBER.create();

        GuildApi.API.modifyMember(source.getLevel(), guild, targetPlayer.getUUID(), MemberStatus.ALLIED);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.add_ally", targetPlayer.getName()), false);
        targetPlayer.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.now_allied", guild.displayName()), false);
    }

    private static void remove(CommandSourceStack source, String targetName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        UUID targetId = TeamArguments.resolveMember(source, guild, targetName);
        if (targetId == null || !guild.isAllied(targetId)) throw TeamExceptions.NOT_ALLY.create();
        remove(source, targetId, TeamArguments.memberName(source.getServer(), guild, targetId));
    }

    public static void remove(CommandSourceStack source, UUID targetId, Component targetName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        if (!guild.canManageMembers(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_MEMBERS.create();
        if (!guild.isAllied(targetId)) throw TeamExceptions.NOT_ALLY.create();

        GuildApi.API.leave(source.getLevel(), guild, targetId);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.remove_ally", targetName), false);
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(targetId);
        if (online != null) {
            online.displayClientMessage(ModUtils.translatableWithStyle("command.argonauts.no_longer_allied", guild.displayName()), false);
        }
    }

    private static void list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();

        GameProfileCache profileCache = source.getServer().getProfileCache();
        if (profileCache == null) return;

        guild.members().forEach((id, member) -> {
            if (member.status() == MemberStatus.ALLIED) {
                profileCache.get(id).ifPresent(profile -> source.sendSuccess(() ->
                    ModUtils.translatableWithStyle("command.argonauts.list_ally", profile.getName()), false));
            }
        });
    }
}
