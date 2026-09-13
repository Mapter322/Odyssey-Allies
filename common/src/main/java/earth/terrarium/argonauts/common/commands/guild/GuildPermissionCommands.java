package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.common.commands.TeamArguments;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import earth.terrarium.argonauts.api.util.ModUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class GuildPermissionCommands {

    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTION_PROVIDER = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) return builder.buildFuture();
        List<String> keys = new ArrayList<>(MemberPermissionsApi.API.getGuildPermissions().keySet());
        MemberSettingsApi.API.getSettings(guild).forEach(setting -> keys.add(setting.id()));
        return SharedSuggestionProvider.suggest(keys.stream()
            .map(key -> key.contains(":") ? "\"" + key + "\"" : key)
            .toList(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("permissions")
                    .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.string())
                            .suggests(KEY_SUGGESTION_PROVIDER)
                            .then(Commands.argument("player", EntityArgument.player())
                                .suggests(TeamSuggestionProviders.CURRENT_GUILD_MEMBERS_SUGGESTION_PROVIDER)
                                .then(Commands.argument("value", StringArgumentType.word())
                                    .suggests(TeamArguments.TRI_STATE_SUGGESTION_PROVIDER)
                                    .executes(context -> {
                                            TriState value = TeamArguments.parseTriState(StringArgumentType.getString(context, "value"));
                                            if (value == null) throw TeamExceptions.INVALID_PERMISSION_VALUE.create();
                                            set(context.getSource(), StringArgumentType.getString(context, "key"),
                                                EntityArgument.getPlayer(context, "player"), value);
                                            return 1;
                                    })
                                )
                            )
                        )
                    )
                    .then(Commands.literal("get")
                        .then(Commands.argument("key", StringArgumentType.string())
                            .suggests(KEY_SUGGESTION_PROVIDER)
                            .then(Commands.argument("player", EntityArgument.player())
                                .suggests(TeamSuggestionProviders.CURRENT_GUILD_MEMBERS_SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    get(context.getSource(), StringArgumentType.getString(context, "key"),
                                        EntityArgument.getPlayer(context, "player"));
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                            .suggests(TeamSuggestionProviders.CURRENT_GUILD_MEMBERS_SUGGESTION_PROVIDER)
                            .executes(context -> {
                                list(context.getSource(), EntityArgument.getPlayer(context, "player"));
                                return 1;
                            })
                        )
                    )
                )
            ));
    }

    private static void set(CommandSourceStack source, String key, ServerPlayer target, TriState value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        if (!guild.canManagePermissions(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_PERMISSIONS.create();

        Component name = keyName(guild, key);
        if (name == null) throw TeamExceptions.PERMISSION_NOT_FOUND.create();

        GuildApi.API.modifyPermission(source.getLevel(), guild, target.getUUID(), key, value);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.permissions.set",
            name,
            TeamArguments.triStateName(value),
            target.getName()
        ), false);
    }

    private static void get(CommandSourceStack source, String key, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();

        Component name = keyName(guild, key);
        if (name == null) throw TeamExceptions.PERMISSION_NOT_FOUND.create();

        boolean value = MemberPermissionsApi.API.getGuildPermissions().containsKey(key)
            ? guild.hasPermission(target.getUUID(), key)
            : guild.getPermission(target.getUUID(), key) == TriState.TRUE;

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.permissions.get",
            name,
            value,
            target.getName()
        ), false);
    }

    private static void list(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();

        Member member = guild.getOrCreateMember(target.getUUID());

        member.permissionOverrides().forEach((key, value) -> source.sendSuccess(() -> {
            Component name = keyName(guild, key);
            return ModUtils.translatableWithStyle("command.argonauts.permissions.list",
                name == null ? Component.literal(key) : name,
                TeamArguments.triStateName(value)
            );
        }, false));
    }

    private static Component keyName(Guild guild, String key) {
        if (MemberPermissionsApi.API.getGuildPermissions().containsKey(key)) {
            return MemberPermissionsApi.API.getPermissionName(key);
        }
        return MemberSettingsApi.API.getSettings(guild).stream()
            .filter(setting -> setting.id().equals(key))
            .map(MemberSetting::name)
            .findFirst()
            .orElse(null);
    }
}
