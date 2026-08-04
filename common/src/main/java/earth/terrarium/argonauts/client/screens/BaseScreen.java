package earth.terrarium.argonauts.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.client.screens.BaseCursorScreen;
import com.teamresourceful.resourcefullib.client.screens.ScreenHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public abstract class BaseScreen extends BaseCursorScreen implements ScreenHistory {

    protected static final int CLOSE_X = 8;
    protected static final int CLOSE_Y = 6;
    protected static final int CLOSE_SIZE = 10;

    protected int imageWidth;
    protected int imageHeight;
    protected int leftPos;
    protected int topPos;
    protected int titleLabelX;
    protected int titleLabelY;

    @Nullable
    private Screen lastScreen;

    public BaseScreen(Component displayName, int imageWidth, int imageHeight) {
        super(displayName);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.titleLabelX = 18;
        this.titleLabelY = 6;
        this.lastScreen = Minecraft.getInstance().screen;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.renderBg(graphics, partialTick, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(this.leftPos, this.topPos, 0.0F);
            this.renderLabels(graphics, mouseX, mouseY);

            int relMx = mouseX - this.leftPos;
            int relMy = mouseY - this.topPos;
            boolean closeHovered = relMx >= CLOSE_X && relMx < CLOSE_X + CLOSE_SIZE
                && relMy >= CLOSE_Y && relMy < CLOSE_Y + CLOSE_SIZE;
            graphics.drawString(font, "\u2715", CLOSE_X, CLOSE_Y, closeHovered ? 0xFFFFFF : 0xAAAAAA, false);
        }
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double relMx = mx - this.leftPos;
        double relMy = my - this.topPos;
        if (relMx >= CLOSE_X && relMx < CLOSE_X + CLOSE_SIZE
            && relMy >= CLOSE_Y && relMy < CLOSE_Y + CLOSE_SIZE) {
            if (this.canGoBack()) {
                this.goBack();
            } else {
                this.onClose();
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void setLastScreen(@Nullable Screen screen) {
        this.lastScreen = screen;
    }

    @Override
    @Nullable
    public Screen getLastScreen() {
        return this.lastScreen;
    }

    @Override
    public boolean canGoBack() {
        return this.lastScreen != null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.canGoBack()) {
            this.goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected abstract void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY);

    protected abstract void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);
}
