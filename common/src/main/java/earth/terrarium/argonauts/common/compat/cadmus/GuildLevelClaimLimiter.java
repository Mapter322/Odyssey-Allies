package earth.terrarium.argonauts.common.compat.cadmus;

import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.common.settings.Settings;
import earth.terrarium.argonauts.common.utils.Config;
import earth.terrarium.cadmus.api.claims.limit.ClaimLimiter;
import earth.terrarium.cadmus.api.teams.TeamId;
import net.minecraft.server.MinecraftServer;

public class GuildLevelClaimLimiter implements ClaimLimiter {

    @Override
    public int getMaxClaims(MinecraftServer server, TeamId id) {
        if (!ArgonautsTeam.ID.equals(id.provider())) return Integer.MAX_VALUE;
        return GuildApi.API.get(server.overworld(), id.id())
            .map(guild -> Config.getMaxClaims(Settings.LEVEL.get(guild)))
            .filter(limit -> limit > 0)
            .orElse(Integer.MAX_VALUE);
    }

    @Override
    public int getMaxChunkLoadedClaims(MinecraftServer server, TeamId id) {
        if (!ArgonautsTeam.ID.equals(id.provider())) return Integer.MAX_VALUE;
        return GuildApi.API.get(server.overworld(), id.id())
            .map(guild -> Config.getMaxForceloads(Settings.LEVEL.get(guild)))
            .filter(limit -> limit > 0)
            .orElse(Integer.MAX_VALUE);
    }
}
