package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.settings.types.IntegerSetting;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.settings.Settings;
import earth.terrarium.argonauts.common.utils.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;

import java.util.UUID;

public class GuildLevelCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("admin")
                    .then(Commands.literal("level")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                            .then(Commands.argument("id", UuidArgument.uuid())
                                .suggests(GuildAdminCommands.GUILDS_SUGGESTION_PROVIDER)
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                    .executes(context -> {
                                        set(context.getSource(), UuidArgument.getUuid(context, "id"), IntegerArgumentType.getInteger(context, "level"));
                                        return 1;
                                    })
                                )
                            )
                        )
                        .then(Commands.literal("get")
                            .then(Commands.argument("id", UuidArgument.uuid())
                                .suggests(GuildAdminCommands.GUILDS_SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    get(context.getSource(), UuidArgument.getUuid(context, "id"));
                                    return 1;
                                })
                            )
                        )
                    )
                )
            ));
    }

    private static void set(CommandSourceStack source, UUID id, int level) throws CommandSyntaxException {
        Guild guild = GuildApi.API.get(source.getLevel(), id).orElse(null);
        if (guild == null) throw TeamExceptions.GUILD_DOES_NOT_EXIST.create();
        if (!Config.hasLevel(level)) throw TeamExceptions.INVALID_GUILD_LEVEL.create();

        GuildApi.API.modifySetting(source.getLevel(), guild, new IntegerSetting(Settings.LEVEL.id(), level), Settings.LEVEL.id());
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.level.set", level, guild.displayName()), false);
    }

    private static void get(CommandSourceStack source, UUID id) throws CommandSyntaxException {
        Guild guild = GuildApi.API.get(source.getLevel(), id).orElse(null);
        if (guild == null) throw TeamExceptions.GUILD_DOES_NOT_EXIST.create();
        int level = Settings.LEVEL.get(guild);
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.level.get", guild.displayName(), level), false);
    }
}
