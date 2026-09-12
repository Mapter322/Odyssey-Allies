package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.guild.GuildRoleApi;
import earth.terrarium.argonauts.api.teams.guild.Role;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.argonauts.common.commands.TeamArguments;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import earth.terrarium.argonauts.common.guild.GuildRoleDefaults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class GuildRoleCommands {

    private static final Pattern ROLE_NAME = Pattern.compile("^[a-z0-9_-]{1,24}$");

    private static final SuggestionProvider<CommandSourceStack> ROLE_SUGGESTION_PROVIDER = (context, builder) -> {
        Guild guild = getGuild(context.getSource());
        if (guild == null) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(guild.roles().keySet(), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PARENT_SUGGESTION_PROVIDER = (context, builder) -> {
        Guild guild = getGuild(context.getSource());
        if (guild == null) return builder.buildFuture();
        List<String> options = new ArrayList<>(List.of("none"));
        options.addAll(guild.roles().keySet());
        return SharedSuggestionProvider.suggest(options, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PERMISSION_SUGGESTION_PROVIDER = (context, builder) ->
        SharedSuggestionProvider.suggest(MemberPermissionsApi.API.getGuildPermissions().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> SETTING_SUGGESTION_PROVIDER = (context, builder) -> {
        Guild guild = getGuild(context.getSource());
        if (guild == null) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(MemberSettingsApi.API.getSettings(guild).stream().map(MemberSetting::id).toList(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("role")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                create(context.getSource(), StringArgumentType.getString(context, "name"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("delete")
                        .then(Commands.argument("role", StringArgumentType.word())
                            .suggests(ROLE_SUGGESTION_PROVIDER)
                            .executes(context -> {
                                delete(context.getSource(), roleId(context));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("parent")
                        .then(Commands.argument("role", StringArgumentType.word())
                            .suggests(ROLE_SUGGESTION_PROVIDER)
                            .then(Commands.argument("parent", StringArgumentType.word())
                                .suggests(PARENT_SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    parent(context.getSource(), roleId(context), StringArgumentType.getString(context, "parent"));
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("assign")
                        .then(Commands.argument("player", EntityArgument.player())
                            .suggests(TeamSuggestionProviders.CURRENT_GUILD_MEMBERS_SUGGESTION_PROVIDER)
                            .then(Commands.argument("role", StringArgumentType.word())
                                .suggests(ROLE_SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    assign(context.getSource(), EntityArgument.getPlayer(context, "player"), roleId(context));
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("permission")
                        .then(Commands.argument("role", StringArgumentType.word())
                            .suggests(ROLE_SUGGESTION_PROVIDER)
                            .then(Commands.argument("permission", StringArgumentType.word())
                                .suggests(PERMISSION_SUGGESTION_PROVIDER)
                                .then(Commands.argument("value", StringArgumentType.word())
                                    .suggests(TeamArguments.TRI_STATE_SUGGESTION_PROVIDER)
                                    .executes(context -> {
                                        permission(context.getSource(), roleId(context), StringArgumentType.getString(context, "permission"),
                                            value(context));
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                    .then(Commands.literal("setting")
                        .then(Commands.argument("role", StringArgumentType.word())
                            .suggests(ROLE_SUGGESTION_PROVIDER)
                            .then(Commands.argument("setting", StringArgumentType.word())
                                .suggests(SETTING_SUGGESTION_PROVIDER)
                                .then(Commands.argument("value", StringArgumentType.word())
                                    .suggests(TeamArguments.TRI_STATE_SUGGESTION_PROVIDER)
                                    .executes(context -> {
                                        setting(context.getSource(), roleId(context), StringArgumentType.getString(context, "setting"),
                                            value(context));
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                )
            ));
    }

    private static void create(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuildAndPermission(source, player);

        String roleId = name.toLowerCase(Locale.ROOT);
        if (!ROLE_NAME.matcher(roleId).matches() || roleId.equals("none")) throw TeamExceptions.INVALID_ROLE_NAME.create();
        if (guild.roles().containsKey(roleId)) throw TeamExceptions.ROLE_ALREADY_EXISTS.create();

        GuildRoleApi.API.modifyRole(source.getLevel(), guild, new Role(roleId, Role.ALL));
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.role.create", roleId), false);
    }

    private static void delete(CommandSourceStack source, String roleId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuildAndPermission(source, player);

        if (GuildRoleDefaults.isDefaultRole(roleId)) throw TeamExceptions.CANNOT_DELETE_DEFAULT_ROLE.create();
        if (!guild.roles().containsKey(roleId)) throw TeamExceptions.ROLE_NOT_FOUND.create();

        GuildRoleApi.API.removeRole(source.getLevel(), guild, roleId);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.role.delete", roleId), false);
    }

    private static void parent(CommandSourceStack source, String roleId, String parent) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuildAndPermission(source, player);

        Role role = guild.roles().get(roleId);
        if (role == null) throw TeamExceptions.ROLE_NOT_FOUND.create();
        if (roleId.equals(Role.ALL)) throw TeamExceptions.ROLE_CANNOT_HAVE_PARENT.create();

        String parentId = parent.toLowerCase(Locale.ROOT);
        if (parentId.equals("none")) parentId = "";
        if (!parentId.isEmpty() && !guild.roles().containsKey(parentId)) throw TeamExceptions.ROLE_NOT_FOUND.create();
        if (guild.wouldCreateRoleCycle(roleId, parentId)) throw TeamExceptions.ROLE_CYCLE.create();

        role.setParent(parentId);
        GuildRoleApi.API.modifyRole(source.getLevel(), guild, role);
        String resolved = parentId.isEmpty() ? "none" : parentId;
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.role.parent", roleId, resolved), false);
    }

    private static void assign(CommandSourceStack source, ServerPlayer target, String roleId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuildAndPermission(source, player);

        if (!guild.isMember(target.getUUID())) throw TeamExceptions.PLAYER_NOT_IN_GUILD.create();
        if (!guild.roles().containsKey(roleId)) throw TeamExceptions.ROLE_NOT_FOUND.create();
        if (roleId.equals(Role.ALL) || roleId.equals(Role.ALLY)) throw TeamExceptions.CANNOT_ASSIGN_ROLE.create();

        GuildRoleApi.API.modifyMemberRole(source.getLevel(), guild, target.getUUID(), roleId);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.role.assign", roleId, target.getName()), false);
    }

    private static void permission(CommandSourceStack source, String roleId, String permission, TriState value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuildAndPermission(source, player);

        Role role = guild.roles().get(roleId);
        if (role == null) throw TeamExceptions.ROLE_NOT_FOUND.create();
        if (!MemberPermissionsApi.API.getGuildPermissions().containsKey(permission)) throw TeamExceptions.PERMISSION_NOT_FOUND.create();

        role.setOverride(permission, value);
        GuildRoleApi.API.modifyRole(source.getLevel(), guild, role);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.role.permission",
            roleId,
            MemberPermissionsApi.API.getPermissionName(permission),
            TeamArguments.triStateName(value)
        ), false);
    }

    private static void setting(CommandSourceStack source, String roleId, String settingId, TriState value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuildAndPermission(source, player);

        Role role = guild.roles().get(roleId);
        if (role == null) throw TeamExceptions.ROLE_NOT_FOUND.create();
        MemberSetting setting = MemberSettingsApi.API.getSettings(guild).stream()
            .filter(entry -> entry.id().equals(settingId))
            .findFirst()
            .orElseThrow(() -> TeamExceptions.SETTING_NOT_FOUND.create());

        role.setOverride(setting.id(), value);
        GuildRoleApi.API.modifyRole(source.getLevel(), guild, role);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.role.setting",
            roleId,
            setting.name(),
            TeamArguments.triStateName(value)
        ), false);
    }

    private static Guild requireGuildAndPermission(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        if (!guild.canManagePermissions(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_PERMISSIONS.create();
        return guild;
    }

    private static String roleId(CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, "role").toLowerCase(Locale.ROOT);
    }

    private static TriState value(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        TriState value = TeamArguments.parseTriState(StringArgumentType.getString(context, "value"));
        if (value == null) throw TeamExceptions.INVALID_PERMISSION_VALUE.create();
        return value;
    }

    @Nullable
    private static Guild getGuild(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
        return GuildApi.API.getPlayerGuild(player).orElse(null);
    }
}
