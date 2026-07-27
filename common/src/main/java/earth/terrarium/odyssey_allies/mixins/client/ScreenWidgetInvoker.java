package earth.terrarium.odyssey_allies.mixins.client;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code Screen#addRenderableWidget} so we can add widgets to any vanilla
 * screen (like {@link net.minecraft.client.gui.screens.inventory.InventoryScreen})
 * the exact same way the screen adds its own widgets.
 */
@Mixin(Screen.class)
public interface ScreenWidgetInvoker {

    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T odysseyAllies$addRenderableWidget(T widget);
}
