package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.config.RoleDefaultsConfig;
import earth.terrarium.argonauts.common.utils.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class GuildConditionCommands {

    private static final SuggestionProvider<CommandSourceStack> ROLE_SUGGESTION_PROVIDER = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(guild.roles().keySet(), builder);
    };

    private static final SuggestionProvider<CommandSourceStack> CONDITION_SUGGESTION_PROVIDER = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) return builder.buildFuture();
        String role = StringArgumentType.getString(context, "role");
        return SharedSuggestionProvider.suggest(guild.getConditions(role).stream()
            .map(condition -> "\"" + condition + "\"")
            .toList(), builder);
    };

    private GuildConditionCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("condition")
            .then(Commands.literal("add")
                .then(Commands.argument("role", StringArgumentType.word())
                    .suggests(ROLE_SUGGESTION_PROVIDER)
                    .then(Commands.argument("condition", StringArgumentType.string())
                        .executes(GuildConditionCommands::add)
                    )
                )
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("role", StringArgumentType.word())
                    .suggests(ROLE_SUGGESTION_PROVIDER)
                    .then(Commands.argument("condition", StringArgumentType.string())
                        .suggests(CONDITION_SUGGESTION_PROVIDER)
                        .executes(GuildConditionCommands::remove)
                    )
                )
            )
            .then(Commands.literal("list")
                .then(Commands.argument("role", StringArgumentType.word())
                    .suggests(ROLE_SUGGESTION_PROVIDER)
                    .executes(GuildConditionCommands::list)
                )
            );
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuild(player);
        String role = requireRole(guild, StringArgumentType.getString(context, "role"));

        String condition = StringArgumentType.getString(context, "condition");
        if (!isValid(condition)) throw TeamExceptions.INVALID_CONDITION.create();

        boolean removed = guild.getRemovedConditions(role).contains(condition);
        if (removed) {
            GuildApi.API.restoreDefaultCondition(source.getLevel(), guild, role, condition);
        }
        if (guild.getConditions(role).contains(condition)) {
            throw TeamExceptions.CONDITION_ALREADY_EXISTS.create();
        }
        if (isGlobal(condition)) {
            if (removed) {
                source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.condition.add", Component.literal(condition), Component.literal(role)), false);
                return 1;
            }
            throw TeamExceptions.CONDITION_ALREADY_EXISTS.create();
        }
        if (guild.getConditions().size() >= Config.maxGuildConditions) {
            throw TeamExceptions.CONDITION_LIMIT.create();
        }

        GuildApi.API.addCondition(source.getLevel(), guild, role, condition);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.condition.add", Component.literal(condition), Component.literal(role)), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuild(player);
        String role = requireRole(guild, StringArgumentType.getString(context, "role"));

        String condition = StringArgumentType.getString(context, "condition");
        if (guild.getConditions(role).contains(condition)) {
            GuildApi.API.removeCondition(source.getLevel(), guild, role, condition);
            source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.condition.remove", Component.literal(condition), Component.literal(role)), false);
            return 1;
        }
        if (!guild.getConditions().contains(condition)
            && !guild.getRemovedConditions(role).contains(condition)
            && guild.isTargetVisible(role, condition)
            && isKnownCondition(guild, condition)) {
            GuildApi.API.removeDefaultCondition(source.getLevel(), guild, role, condition);
            source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.condition.remove", Component.literal(condition), Component.literal(role)), false);
            return 1;
        }

        throw TeamExceptions.CONDITION_NOT_FOUND.create();
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = requireGuild(player);
        String role = requireRole(guild, StringArgumentType.getString(context, "role"));

        if (guild.getConditions(role).isEmpty()) {
            source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.condition.list_empty"), false);
            return 0;
        }
        for (String condition : guild.getConditions(role)) {
            source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.condition.list", Component.literal(condition)), false);
        }
        return guild.getConditions(role).size();
    }

    private static Guild requireGuild(ServerPlayer player) throws CommandSyntaxException {
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        if (!guild.canManagePermissions(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_PERMISSIONS.create();
        return guild;
    }

    private static String requireRole(Guild guild, String role) throws CommandSyntaxException {
        if (!guild.roles().containsKey(role)) throw TeamExceptions.ROLE_NOT_FOUND.create();
        return role;
    }

    private static boolean isGlobal(String condition) {
        return MemberSettingsApi.API.getSettings(null).stream().anyMatch(setting -> setting.id().equals(condition));
    }

    private static boolean isKnownCondition(Guild guild, String condition) {
        return MemberSettingsApi.API.getSettings(guild).stream()
            .anyMatch(setting -> setting.hasParent() && setting.id().equals(condition));
    }

    private static boolean isValid(String condition) {
        int index = condition.indexOf('/');
        if (index <= 0 || index >= condition.length() - 1) return false;
        if (!RoleDefaultsConfig.CONDITION_PARENTS.contains(condition.substring(0, index))) return false;
        String child = condition.substring(index + 1);
        ResourceLocation location = ResourceLocation.tryParse(child.startsWith("#") ? child.substring(1) : child);
        return location != null;
    }
}
