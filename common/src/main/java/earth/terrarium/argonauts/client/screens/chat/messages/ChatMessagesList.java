package earth.terrarium.argonauts.client.screens.chat.messages;

import earth.terrarium.olympus.client.components.base.ListWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

public class ChatMessagesList extends ListWidget {

    public ChatMessagesList(int width, int height) {
        super(width, height);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener listener) {
        double scroll = this.scroll;
        super.setFocused(listener);
        this.scroll = scroll;
    }

    public boolean isAtBottom() {
        return this.scroll >= Math.max(0, this.contentHeight() - this.getHeight()) - 1;
    }

    public void scrollToBottom() {
        this.scroll = Math.max(0, this.contentHeight() - this.getHeight());
    }

    public int itemCount() {
        return this.items.size();
    }

    private int contentHeight() {
        int height = 0;
        for (AbstractWidget item : this.items) {
            height += item.getHeight() + this.gap;
        }
        return height;
    }
}
