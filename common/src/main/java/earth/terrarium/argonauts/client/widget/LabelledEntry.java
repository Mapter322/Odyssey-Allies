package earth.terrarium.argonauts.client.widget;

import earth.terrarium.olympus.client.components.base.BaseParentWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class LabelledEntry extends BaseParentWidget {
    private final Component label;
    private final AbstractWidget entry;
    private final Font font;

    private float alignment = 0.5F;
    private int color = 0xFFFFFF;
    private int leftPadding = 2;
    private boolean lockedWidth = false;
    private int entryYOffset = 0;
    private boolean drawDivider = false;
    private int dividerYOffset = 0;
    private int dividerColor = 0x20FFFFFF;

    public LabelledEntry(Font font, Component label, AbstractWidget entry) {
        super(0, Math.max(font.lineHeight, entry.getHeight()));
        this.font = font;
        this.label = label;
        this.entry = entry;
        this.children.add(entry);
    }

    public LabelledEntry alignTop() {
        this.alignment = 0.0F;
        return this;
    }

    public LabelledEntry alignBottom() {
        this.alignment = 1.0F;
        return this;
    }

    public LabelledEntry alignCenter() {
        this.alignment = 0.5F;
        return this;
    }

    public LabelledEntry setColor(int color) {
        this.color = color;
        return this;
    }

    public LabelledEntry setLeftPadding(int leftPadding) {
        this.leftPadding = leftPadding;
        return this;
    }

    public LabelledEntry setLockedWidth() {
        this.lockedWidth = true;
        return this;
    }

    public LabelledEntry setEntryYOffset(int offset) {
        this.entryYOffset = offset;
        return this;
    }

    public LabelledEntry setDividerYOffset(int offset) {
        this.dividerYOffset = offset;
        return this;
    }

    public LabelledEntry setDrawDivider(boolean drawDivider) {
        this.drawDivider = drawDivider;
        return this;
    }

    public LabelledEntry setDividerColor(int dividerColor) {
        this.dividerColor = dividerColor;
        return this;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.entry.setX(x + this.width - this.entry.getWidth());
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.entry.setY(y + (int) ((this.height - this.entry.getHeight()) * this.alignment) + entryYOffset);
    }

    public int getTextY() {
        return this.getY() + (int) ((this.height - this.font.lineHeight) * this.alignment);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (!this.lockedWidth) {
            this.entry.setWidth(width / 2);
        }
        this.entry.setX(this.getX() + this.getWidth() - this.entry.getWidth());
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.drawString(this.font, this.label, this.getX() + leftPadding, this.getTextY(), this.color);
        entry.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (this.drawDivider) {
            int y = this.getY() + this.getHeight() + this.dividerYOffset;
            int startX = this.getX();
            int endX = this.getX() + this.getWidth();
            guiGraphics.fill(startX, y, endX, y + 1, this.dividerColor);
        }
    }
}
