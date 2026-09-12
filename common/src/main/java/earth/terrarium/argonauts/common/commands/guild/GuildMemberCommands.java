package earth.terrarium.argonauts.common.commands.guild;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.permissions.MemberPermissionsApi;
import earth.terrarium.argonauts.api.teams.settings.MemberSetting;
import earth.terrarium.argonauts.api.teams.settings.MemberSettingsApi;
import earth.terrarium.argonauts.api.util.ModUtils;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import earth.terrarium.argonauts.common.commands.TeamSuggestionProviders;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GuildMemberCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("argonauts")
            .then(Commands.literal("guild")
                .then(Commands.literal("members")
                    .then(Commands.literal("list")
                        .executes(context -> {
                            list(context.getSource());
                            return 1;
                        })
                    )
                    .then(Commands.literal("info")
                        .then(Commands.argument("player", EntityArgument.player())
                            .suggests(TeamSuggestionProviders.CURRENT_GUILD_MEMBERS_SUGGESTION_PROVIDER)
                            .executes(context -> {
                                info(context.getSource(), EntityArgument.getPlayer(context, "player"));
                                return 1;
                            })
                        )
                    )
                    .executes(context -> {
                        list(context.getSource());
                        return 1;
                    })
                )
            ));
    }

    private static void list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();

        GameProfileCache profileCache = source.getServer().getProfileCache();
        if (profileCache == null) return;

        guild.members().forEach((id, member) ->
            profileCache.get(id).ifPresent(profile -> source.sendSuccess(() ->
                ModUtils.translatableWithStyle("command.argonauts.list_member", profile.getName(), member.status().getDisplayName()), false)));
    }

    private static void info(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Guild guild = GuildApi.API.getPlayerGuild(player).orElse(null);
        if (guild == null) throw TeamExceptions.NOT_IN_GUILD.create();
        if (!guild.canManagePermissions(player.getUUID())) throw TeamExceptions.NO_PERMISSION_MANAGE_PERMISSIONS.create();

        Member member = guild.members().get(target.getUUID());
        if (member == null || !member.status().isMember()) throw TeamExceptions.PLAYER_NOT_IN_GUILD.create();

        String roleId = guild.getRoleId(target.getUUID());
        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.member.info.name",
            target.getName(),
            Component.translatableWithFallback("gui.argonauts.role." + roleId, roleId)), false);

        source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.member.info.guild_permissions"), false);
        List<String> permissions = new ArrayList<>(MemberPermissionsApi.API.getGuildPermissions().keySet());
        permissions.sort(String::compareTo);
        for (String permission : permissions) {
            boolean allowed = member.isOwner() || guild.getPermission(member, permission) == TriState.TRUE;
            source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.member.info.entry",
                MemberPermissionsApi.API.getPermissionName(permission), allowed), false);
        }

        List<MemberSetting> settings = new ArrayList<>(MemberSettingsApi.API.getSettings(guild));
        if (!settings.isEmpty()) {
            settings.sort(Comparator.comparing(MemberSetting::id));
            source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.member.info.claim_permissions"), false);
            for (MemberSetting setting : settings) {
                TriState state = guild.getRoleValue(target.getUUID(), setting.id());
                String value = switch (state) {
                    case TRUE -> "allow";
                    case FALSE -> "deny";
                    case UNDEFINED -> "none";
                };
                source.sendSuccess(() -> ModUtils.translatableWithStyle("command.argonauts.member.info.entry",
                    setting.name(), value), false);
            }
        }
    }
}
