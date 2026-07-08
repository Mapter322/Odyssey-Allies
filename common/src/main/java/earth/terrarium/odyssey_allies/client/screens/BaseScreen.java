package earth.terrarium.odyssey_allies.client.screens;

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

    protected final int imageWidth;
    protected final int imageHeight;
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
        this.titleLabelX = 8;
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
        }
        RenderSystem.enableDepthTest();
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
