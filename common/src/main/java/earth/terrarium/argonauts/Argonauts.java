package earth.terrarium.argonauts;

import com.teamresourceful.resourcefullib.common.utils.modinfo.ModInfoUtils;
import com.mojang.authlib.GameProfile;
import earth.terrarium.argonauts.api.teams.Member;
import earth.terrarium.argonauts.api.teams.Team;
import earth.terrarium.argonauts.api.teams.guild.Guild;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.party.Party;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.common.compat.cadmus.CadmusCompat;
import earth.terrarium.argonauts.common.compat.heracles.HeraclesCompat;

import earth.terrarium.argonauts.common.config.RoleDefaultsConfig;
import earth.terrarium.argonauts.common.constants.ConstantComponents;
import earth.terrarium.argonauts.common.guild.GuildRoleDefaults;
import earth.terrarium.argonauts.common.guild.GuildSaveData;
import earth.terrarium.argonauts.common.network.NetworkHandler;
import earth.terrarium.argonauts.common.network.packets.ClientboundSyncGuildsPacket;
import earth.terrarium.argonauts.common.network.packets.ClientboundSyncPartiesPacket;
import earth.terrarium.argonauts.common.party.PartySaveData;
import earth.terrarium.argonauts.common.permissions.Permissions;
import earth.terrarium.argonauts.common.settings.Settings;
import earth.terrarium.argonauts.common.utils.Config;
import earth.terrarium.argonauts.common.utils.ModUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;

import java.util.Map;
import java.util.UUID;

public class Argonauts {

    public static final String MOD_ID = "argonauts";

    public static final boolean IS_CLAIMS_LOADED = ModInfoUtils.isModLoaded("cadmus");
    public static final boolean IS_QUESTS_LOADED = ModInfoUtils.isModLoaded("odyssey_quests");

    public static final int MIN_PARTY_MEMBERS = 2;
    public static final int MAX_PARTY_MEMBERS = 8;
    public static final int DEFAULT_MAX_PARTY_MEMBERS = 4;

    public static void init() {
        NetworkHandler.init();
        Config.load();
        Settings.init();
        Permissions.init();
        if (IS_CLAIMS_LOADED) CadmusCompat.init();
        if (IS_QUESTS_LOADED) HeraclesCompat.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void onServerStarted(MinecraftServer server) {
        RoleDefaultsConfig.ensureLoaded();
        GuildSaveData data = GuildSaveData.read(server.overworld());
        boolean changed = false;
        for (Guild guild : data.guilds().values()) {
            changed |= GuildRoleDefaults.applyMissingDefaults(guild, guild.roles());
            changed |= resolveNames(server, guild);
        }
        if (changed) {
            data.markDirty();
        }

        PartySaveData partyData = PartySaveData.read(server.overworld());
        boolean partiesChanged = false;
        for (Party party : partyData.parties().values()) {
            partiesChanged |= resolveNames(server, party);
        }
        if (partiesChanged) {
            partyData.setDirty();
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.server;
        var guilds = GuildApi.API.getAll(player.level());
        if (guilds.stream().anyMatch(guild -> resolveNames(server, guild))) {
            GuildSaveData.read(server.overworld()).markDirty();
        }
        var parties = PartyApi.API.getAll(player.level());
        if (parties.stream().anyMatch(party -> resolveNames(server, party))) {
            PartySaveData.read(server.overworld()).setDirty();
        }

        if (NetworkHandler.CHANNEL.canSendToPlayer(player, ClientboundSyncGuildsPacket.TYPE)) {
            NetworkHandler.CHANNEL.sendToPlayer(new ClientboundSyncGuildsPacket(guilds), player);
        }

        if (NetworkHandler.CHANNEL.canSendToPlayer(player, ClientboundSyncPartiesPacket.TYPE)) {
            NetworkHandler.CHANNEL.sendToPlayer(new ClientboundSyncPartiesPacket(parties), player);
        }

        GuildApi.API.getPlayerGuild(player).ifPresent(guild -> {
            String motd = Settings.MOTD.get(guild);
            if (!motd.isBlank()) {
                player.displayClientMessage(ConstantComponents.MOTD_HEADER, false);
                player.displayClientMessage(ModUtils.getParsedComponent(Component.literal(motd), player), false);
                player.displayClientMessage(ConstantComponents.MOTD_LINE, false);
            }
        });
    }

    /**
     * Fills in missing member names from the server profile cache, so offline members
     * are displayed with their name instead of their UUID.
     */
    private static boolean resolveNames(MinecraftServer server, Team team) {
        GameProfileCache cache = server.getProfileCache();
        if (cache == null) return false;
        boolean changed = false;
        for (Map.Entry<UUID, Member> entry : team.members().entrySet()) {
            Member member = entry.getValue();
            if (!member.name().isEmpty()) continue;
            String name = cache.get(entry.getKey()).map(GameProfile::getName).orElse(null);
            if (name == null) continue;
            member.setName(name);
            changed = true;
        }
        return changed;
    }

    public static void onPlayerLeave(ServerPlayer player) {
    }
}