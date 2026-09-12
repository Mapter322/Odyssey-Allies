package earth.terrarium.argonauts.common.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.teamresourceful.resourcefullib.common.utils.TriState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

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
}
