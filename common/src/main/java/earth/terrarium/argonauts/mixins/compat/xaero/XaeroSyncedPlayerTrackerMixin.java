package earth.terrarium.argonauts.mixins.compat.xaero;

import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
    "xaero.common.server.mods.argonauts.ArgonautsSyncedPlayerTrackerSystem",
    "xaero.map.server.mods.argonauts.ArgonautsSyncedPlayerTrackerSystem"
})
public abstract class XaeroSyncedPlayerTrackerMixin {

    @Inject(method = "getPartyTrackingLevel", at = @At("HEAD"), cancellable = true)
    private void argonauts$getPartyTrackingLevel(Player tracker, Player tracked, CallbackInfoReturnable<Integer> cir) {
        var trackerParty = PartyApi.API.getPlayerParty(tracker.level(), tracker.getUUID()).orElse(null);
        if (trackerParty == null) {
            cir.setReturnValue(0);
            return;
        }

        var trackedParty = PartyApi.API.getPlayerParty(tracked.level(), tracked.getUUID()).orElse(null);
        cir.setReturnValue(trackerParty == trackedParty ? 2 : 0);
    }

    @Inject(method = "getGuildTrackingLevel", at = @At("HEAD"), cancellable = true)
    private void argonauts$getGuildTrackingLevel(Player tracker, Player tracked, CallbackInfoReturnable<Integer> cir) {
        MinecraftServer server = tracker.getServer();
        if (server == null) {
            cir.setReturnValue(0);
            return;
        }

        var trackerGuild = GuildApi.API.getPlayerGuild(server.overworld(), tracker.getUUID()).orElse(null);
        if (trackerGuild == null) {
            cir.setReturnValue(0);
            return;
        }

        var trackedGuild = GuildApi.API.getPlayerGuild(server.overworld(), tracked.getUUID()).orElse(null);
        cir.setReturnValue(trackerGuild == trackedGuild ? 2 : 0);
    }
}