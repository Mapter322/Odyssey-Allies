package earth.terrarium.argonauts.common.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TeamArguments {

    public static final SuggestionProvider<CommandSourceStack> TRI_STATE_SUGGESTION_PROVIDER = (context, builder) ->
        SharedSuggestionProvider.suggest(List.of("allow", "deny", "inherit"), builder);

    private TeamArguments() {
    }

    /**
     * Parses a tristate argument. Legacy boolean values are supported.
     *
     * @param value the value
     * @return the parsed state, or null if the value is invalid
     */
    @Nullable
    public static TriState parseTriState(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "allow", "true" -> TriState.TRUE;
            case "deny", "false" -> TriState.FALSE;
            case "inherit", "undefined" -> TriState.UNDEFINED;
            default -> null;
        };
    }

    public static String triStateName(TriState state) {
        return state.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the display name of a team member, even if the player is offline.
     */
    public static Component memberName(MinecraftServer server, Team team, UUID playerId) {
        Member member = team.members().get(playerId);
        if (member != null && !member.name().isEmpty()) return Component.literal(member.name());
        GameProfileCache cache = server.getProfileCache();
        if (cache != null) {
            String name = cache.get(playerId).map(GameProfile::getName).orElse(null);
            if (name != null) return Component.literal(name);
        }
        return Component.literal(playerId.toString());
    }

    /**
     * Resolves a team member by name, even if the player is offline.
     *
     * @return the member UUID, or null if no member with that name exists
     */
    @Nullable
    public static UUID resolveMember(CommandSourceStack source, Team team, String name) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null && team.members().containsKey(online.getUUID())) return online.getUUID();
        for (Map.Entry<UUID, Member> entry : team.members().entrySet()) {
            Member member = entry.getValue();
            if (!member.name().isEmpty() && member.name().equalsIgnoreCase(name)) return entry.getKey();
        }
        GameProfileCache cache = source.getServer().getProfileCache();
        if (cache != null) {
            UUID id = cache.get(name).map(GameProfile::getId).orElse(null);
            if (id != null && team.members().containsKey(id)) return id;
        }
        return null;
    }

    /**
     * Gets the names of every team member, including offline ones.
     */
    public static List<String> memberNames(MinecraftServer server, Team team) {
        GameProfileCache cache = server.getProfileCache();
        return team.members().entrySet().stream()
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
    }
}
