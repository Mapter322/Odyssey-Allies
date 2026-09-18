package earth.terrarium.argonauts.client.screens.chat.messages;

import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import earth.terrarium.argonauts.client.screens.chat.ChatScreen;
import earth.terrarium.argonauts.common.chat.ChatMessage;
import earth.terrarium.olympus.client.components.base.BaseParentWidget;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessageEntry extends BaseParentWidget {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*");
    private static final int PADDING = 4;
    private static final int HEADER_GAP = 1;

    private final int index;
    private final ChatMessage message;
    private final List<FormattedCharSequence> lines = new ArrayList<>();

    public ChatMessageEntry(int index, ChatMessage message) {
        this.index = index;
        this.message = message;
        this.setWidth(176);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        var font = Minecraft.getInstance().font;
        this.lines.clear();
        this.lines.addAll(font.split(formatComponent(this.message.message()), Math.max(16, width - PADDING * 2)));
        this.setHeight(PADDING * 2 + font.lineHeight * (1 + this.lines.size()) + HEADER_GAP);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.isMouseOver(mouseX, mouseY)) return false;
        Style style = this.styleAt(mouseX, mouseY);
        if (style == null || style.getClickEvent() == null) return false;
        if (style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) {
            Util.getPlatform().openUri(style.getClickEvent().getValue());
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        if (this.index % 2 == 0) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x20FFFFFF);
        }

        int x = this.getX() + PADDING;
        int y = this.getY() + PADDING;
        graphics.drawString(font, this.message.profile().getName() + ":", x, y, 0xFFFFAA00, false);
        y += font.lineHeight + HEADER_GAP;
        for (FormattedCharSequence line : this.lines) {
            graphics.drawString(font, line, x, y, 0xFFFFFF, false);
            y += font.lineHeight;
        }

        if (!this.isMouseOver(mouseX, mouseY)) return;
        Style style = this.styleAt(mouseX, mouseY);
        if (style == null) return;
        if (style.getHoverEvent() != null) {
            Component hover = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT);
            if (hover != null) ScreenUtils.setTooltip(hover);
        } else if (style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL
            && Minecraft.getInstance().screen instanceof ChatScreen screen) {
            screen.setEmbedUrl(style.getClickEvent().getValue());
        }
    }

    @Nullable
    private Style styleAt(double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int line = (int) ((mouseY - (this.getY() + PADDING + font.lineHeight + HEADER_GAP)) / font.lineHeight);
        if (line < 0 || line >= this.lines.size()) return null;
        return font.getSplitter().componentStyleAtWidth(this.lines.get(line), (int) (mouseX - (this.getX() + PADDING)));
    }

    private static Component formatComponent(String text) {
        text = text.replace("\n", " ");
        Matcher matcher = URL_PATTERN.matcher(text);
        MutableComponent component = Component.empty();
        int last = 0;
        while (matcher.find()) {
            String url = matcher.group();
            component.append(Component.literal(text.substring(last, matcher.start())));
            component.append(Component.literal(url).withStyle(style -> style.withUnderlined(true)
                .withColor(net.minecraft.ChatFormatting.BLUE).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
            last = matcher.end();
        }
        component.append(Component.literal(text.substring(last)));
        return component;
    }
}
