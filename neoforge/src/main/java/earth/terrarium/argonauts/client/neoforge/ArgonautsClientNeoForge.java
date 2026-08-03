package earth.terrarium.argonauts.client.neoforge;

import earth.terrarium.argonauts.Argonauts;
import earth.terrarium.argonauts.api.teams.guild.GuildApi;
import earth.terrarium.argonauts.api.teams.party.PartyApi;
import earth.terrarium.argonauts.client.ArgonautsClient;
import earth.terrarium.argonauts.client.screens.chat.ChatScreen;
import earth.terrarium.argonauts.client.screens.info.TeamInfoScreen;
import earth.terrarium.argonauts.client.screens.members.MembersScreen;
import earth.terrarium.argonauts.client.screens.settings.SettingsScreen;
import earth.terrarium.argonauts.common.commands.TeamExceptions;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(value = Argonauts.MOD_ID, dist = Dist.CLIENT)
public class ArgonautsClientNeoForge {

    public ArgonautsClientNeoForge() {
        NeoForge.EVENT_BUS.addListener(ArgonautsClientNeoForge::onClientTick);
        NeoForge.EVENT_BUS.addListener(ArgonautsClientNeoForge::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ArgonautsClientNeoForge::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(ArgonautsClientNeoForge::onScreenInit);
        NeoForge.EVENT_BUS.addListener(ArgonautsClientNeoForge::onMouseClickPre);
        ArgonautsClient.init();
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        ArgonautsClient.clientTick();
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        ArgonautsClient.setupInventoryButtons(event.getScreen());
    }

    public static void onMouseClickPre(ScreenEvent.MouseButtonPressed.Pre event) {
        if (ArgonautsClient.handleInventoryClick(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(ArgonautsClient.KEY_OPEN_PARTY_CHAT);
        event.register(ArgonautsClient.KEY_OPEN_GUILD_CHAT);
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ArgonautsClient.onPlayerLoggedOut();
    }

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register((Commands.literal("guild").then(Commands.literal("chat").executes(context -> {
            if (GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).isEmpty()) throw TeamExceptions.NOT_IN_GUILD.create();
            ChatScreen.openGuild();
            return 0;
        }))));

        event.getDispatcher().register((Commands.literal("party").then(Commands.literal("chat").executes(context -> {
            if (PartyApi.API.getPlayerParty(Minecraft.getInstance().player).isEmpty()) throw TeamExceptions.NOT_IN_PARTY.create();
            ChatScreen.openParty();
            return 0;
        }))));

        event.getDispatcher().register(Commands.literal("gc").executes(context -> {
            if (GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).isEmpty()) throw TeamExceptions.NOT_IN_GUILD.create();
            ChatScreen.openGuild();
            return 0;
        }));

        event.getDispatcher().register((Commands.literal("guild").then(Commands.literal("members").executes(context -> {
            if (GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).isEmpty()) throw TeamExceptions.NOT_IN_GUILD.create();
            MembersScreen.openGuild();
            return 0;
        }))));
        event.getDispatcher().register((Commands.literal("party").then(Commands.literal("members").executes(context -> {
            if (PartyApi.API.getPlayerParty(Minecraft.getInstance().player).isEmpty()) throw TeamExceptions.NOT_IN_PARTY.create();
            MembersScreen.openParty();
            return 0;
        }))));

        event.getDispatcher().register((Commands.literal("guild").then(Commands.literal("settings").executes(context -> {
            if (GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).isEmpty()) throw TeamExceptions.NOT_IN_GUILD.create();
            SettingsScreen.openGuild();
            return 0;
        }))));
        event.getDispatcher().register((Commands.literal("party").then(Commands.literal("settings").executes(context -> {
            if (PartyApi.API.getPlayerParty(Minecraft.getInstance().player).isEmpty()) throw TeamExceptions.NOT_IN_PARTY.create();
            SettingsScreen.openParty();
            return 0;
        }))));

        // Info screen
        event.getDispatcher().register(Commands.literal("guild").executes(context -> {
            GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).ifPresent(guild ->
                Minecraft.getInstance().tell(() ->
                    Minecraft.getInstance().setScreen(new TeamInfoScreen("guild", guild))
                )
            );
            return 0;
        }));
        event.getDispatcher().register(Commands.literal("party").executes(context -> {
            PartyApi.API.getPlayerParty(Minecraft.getInstance().player).ifPresent(party ->
                Minecraft.getInstance().tell(() ->
                    Minecraft.getInstance().setScreen(new TeamInfoScreen("party", party))
                )
            );
            return 0;
        }));
    }
}
