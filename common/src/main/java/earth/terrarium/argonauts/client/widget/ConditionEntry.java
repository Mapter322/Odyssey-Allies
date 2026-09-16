package earth.terrarium.argonauts.client.widget;

import com.teamresourceful.resourcefullib.common.color.Color;
import earth.terrarium.olympus.client.components.Widgets;
import earth.terrarium.olympus.client.components.base.BaseParentWidget;
import earth.terrarium.olympus.client.components.buttons.Button;
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers;
import earth.terrarium.olympus.client.constants.MinecraftColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class ConditionEntry extends BaseParentWidget {

    private static final int HEIGHT = 14;
    private static final int ICON_SIZE = 11;
    private static final int ICON_PADDING = 2;
    private static final int TEXT_GAP = 3;
    private static final int ENTRY_Y_OFFSET = -2;

    private final Font font;
    private final Component label;
    private final int labelColor;
    private final int iconSize;
    private final int iconYOffset;
    private final Button button;
    @Nullable
    private final AbstractWidget entry;

    public ConditionEntry(Font font, Component label, int labelColor, ResourceLocation icon, Color iconColor,
                          @Nullable Component tooltip, boolean enabled, Runnable onPress) {
        this(font, label, labelColor, icon, iconColor, tooltip, enabled, onPress, ICON_SIZE, 0, null);
    }

    public ConditionEntry(Font font, Component label, int labelColor, ResourceLocation icon, Color iconColor,
                          @Nullable Component tooltip, boolean enabled, Runnable onPress, int iconSize, int iconYOffset) {
        this(font, label, labelColor, icon, iconColor, tooltip, enabled, onPress, iconSize, iconYOffset, null);
    }

    public ConditionEntry(Font font, Component label, int labelColor, ResourceLocation icon, Color iconColor,
                          @Nullable Component tooltip, boolean enabled, Runnable onPress, @Nullable AbstractWidget entry) {
        this(font, label, labelColor, icon, iconColor, tooltip, enabled, onPress, ICON_SIZE, 0, entry);
    }

    public ConditionEntry(Font font, Component label, int labelColor, ResourceLocation icon, Color iconColor,
                          @Nullable Component tooltip, boolean enabled, Runnable onPress, int iconSize, int iconYOffset,
                          @Nullable AbstractWidget entry) {
        super(0, HEIGHT);
        this.font = font;
        this.label = label;
        this.labelColor = labelColor;
        this.iconSize = iconSize;
        this.iconYOffset = iconYOffset;
        this.entry = entry;

        this.button = Widgets.button()
            .withRenderer(WidgetRenderers.icon(icon).withColor(enabled ? iconColor : MinecraftColors.DARK_GRAY))
            .withTexture(null)
            .withCallback(onPress);
        this.button.withSize(iconSize);
        if (tooltip != null) this.button.withTooltip(tooltip);
        this.button.active = enabled;
        this.children.add(this.button);
        if (entry != null) this.children.add(entry);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.button.setX(x + ICON_PADDING);
        if (this.entry != null) this.entry.setX(x + this.width - this.entry.getWidth());
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.button.setY(y + (this.height - this.iconSize) / 2 + this.iconYOffset);
        if (this.entry != null) this.entry.setY(y + (this.height - this.entry.getHeight()) / 2 + ENTRY_Y_OFFSET);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(
            this.font,
            this.label,
            this.getX() + ICON_PADDING + ICON_SIZE + TEXT_GAP,
            this.getY() + (this.height - this.font.lineHeight) / 2,
            this.labelColor,
            false
        );
        this.button.render(graphics, mouseX, mouseY, partialTick);
        if (this.entry != null) this.entry.render(graphics, mouseX, mouseY, partialTick);
    }
}
