package earth.terrarium.argonauts.neoforge;

import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.common.commands.ArgonautsCommands;
import earth.terrarium.argonauts.common.hud.PartyHudSync;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Argonauts.MOD_ID)
public class ArgonautsNeoForge {

    public ArgonautsNeoForge() {
        Argonauts.init();
        NeoForge.EVENT_BUS.addListener(ArgonautsNeoForge::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ArgonautsNeoForge::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ArgonautsNeoForge::onServerTick);
        NeoForge.EVENT_BUS.addListener(ArgonautsNeoForge::registerCommands);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        PartyHudSync.tick(event.getServer());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Argonauts.onPlayerJoin(player);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Argonauts.onPlayerLeave(player);
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        ArgonautsCommands.register(event.getDispatcher());
    }
}