package earth.terrarium.odyssey_allies.client;

import com.mojang.blaze3d.platform.InputConstants;
import earth.terrarium.odyssey_allies.OdysseyAllies;
import earth.terrarium.odyssey_allies.api.teams.guild.GuildApi;
import earth.terrarium.odyssey_allies.api.teams.party.PartyApi;
import earth.terrarium.odyssey_allies.client.screens.chat.ChatScreen;
import earth.terrarium.odyssey_allies.client.screens.info.TeamInfoScreen;
import earth.terrarium.odyssey_allies.client.screens.widgets.IconButton;
import earth.terrarium.odyssey_allies.common.chat.ChatHandler;
import earth.terrarium.odyssey_allies.common.constants.ConstantComponents;
import earth.terrarium.odyssey_allies.mixins.client.ScreenWidgetInvoker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OdysseyAlliesClient {

    public static final KeyMapping KEY_OPEN_PARTY_CHAT = new KeyMapping(
        ConstantComponents.KEY_OPEN_PARTY_CHAT.getString(),
        InputConstants.UNKNOWN.getValue(),
        ConstantComponents.ODYSSEY_CATEGORY.getString());
    public static final KeyMapping KEY_OPEN_GUILD_CHAT = new KeyMapping(
        ConstantComponents.KEY_OPEN_GUILD_CHAT.getString(),
        InputConstants.UNKNOWN.getValue(),
        ConstantComponents.ODYSSEY_CATEGORY.getString());

    private static final ResourceLocation GUILD_ICON = OdysseyAllies.id("textures/gui/icons/guild.png");
    private static final ResourceLocation PARTY_ICON = OdysseyAllies.id("textures/gui/icons/party.png");

    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;
    private static final int BUTTON_SIZE = 16;
    private static final int ICON_SIZE = 16;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_MARGIN = 4;
    private static final int BUTTON_Y_OFFSET = 5;

    private static IconButton guildButton;
    private static IconButton partyButton;

    public static void init() {
    }


    public static void setupInventoryButtons(Screen screen) {
        if (!(screen instanceof InventoryScreen)) return;
        if (!(screen instanceof ScreenWidgetInvoker invoker)) return;

        int leftPos = (screen.width - INVENTORY_WIDTH) / 2;
        int topPos = (screen.height - INVENTORY_HEIGHT) / 2 + BUTTON_Y_OFFSET;
        int x = leftPos - BUTTON_SIZE - BUTTON_MARGIN;

        guildButton = new IconButton(
            x, topPos, BUTTON_SIZE, ICON_SIZE, GUILD_ICON,
            ConstantComponents.INVENTORY_GUILD_BUTTON,
            btn -> openGuildMenu()
        );
        partyButton = new IconButton(
            x, topPos + BUTTON_SIZE + BUTTON_GAP, BUTTON_SIZE, ICON_SIZE, PARTY_ICON,
            ConstantComponents.INVENTORY_PARTY_BUTTON,
            btn -> openPartyMenu()
        );

        invoker.odysseyAllies$addRenderableWidget(guildButton);
        invoker.odysseyAllies$addRenderableWidget(partyButton);
    }

    public static boolean handleInventoryClick(Screen screen, double mouseX, double mouseY, int button) {
        if (!(screen instanceof InventoryScreen)) return false;

        if (guildButton != null && guildButton.isMouseOver(mouseX, mouseY)) {
            guildButton.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (partyButton != null && partyButton.isMouseOver(mouseX, mouseY)) {
            partyButton.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    private static void openGuildMenu() {
        GuildApi.API.getPlayerGuild(Minecraft.getInstance().player).ifPresent(guild ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new TeamInfoScreen("guild", guild))
            )
        );
    }

    private static void openPartyMenu() {
        PartyApi.API.getPlayerParty(Minecraft.getInstance().player).ifPresent(party ->
            Minecraft.getInstance().tell(() ->
                Minecraft.getInstance().setScreen(new TeamInfoScreen("party", party))
            )
        );
    }

    public static void clientTick() {
        if (KEY_OPEN_PARTY_CHAT.consumeClick()) ChatScreen.openParty();
        if (KEY_OPEN_GUILD_CHAT.consumeClick()) ChatScreen.openGuild();
    }

    @NotNull
    public static Level level() {
        return Objects.requireNonNull(Minecraft.getInstance().level);
    }

    public static void onPlayerLoggedOut() {
        ChatHandler.clearChannels();
    }
}
