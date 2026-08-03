package earth.terrarium.argonauts.client.screens.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A small square vanilla-styled button that renders an icon texture instead of text.
 * Used for the guild/party shortcut buttons next to the inventory screen.
 */
public class IconButton extends Button {

    private final ResourceLocation icon;
    private final int iconSize;

    public IconButton(int x, int y, int size, int iconSize, ResourceLocation icon, Component tooltip, OnPress onPress) {
        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.iconSize = iconSize;
        this.setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally transparent background: skip the vanilla button panel entirely
        // and just draw a faint highlight on hover/focus, then the icon.
        if (this.isHoveredOrFocused()) {
            graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0x40FFFFFF);
        }

        int x = getX() + (this.width - this.iconSize) / 2;
        int y = getY() + (this.height - this.iconSize) / 2;
        graphics.blit(this.icon, x, y, 0, 0, this.iconSize, this.iconSize, this.iconSize, this.iconSize);
    }
}
