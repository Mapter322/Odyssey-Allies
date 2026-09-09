package earth.terrarium.argonauts.mixins.client;

import earth.terrarium.olympus.client.components.base.BaseWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BaseWidget.class)
public abstract class OlympusBaseWidgetMixin {

    @Inject(method = "playDownSound(Lnet/minecraft/client/sounds/SoundManager;)V", at = @At("HEAD"))
    private void argonauts$playDownSound(SoundManager soundManager, CallbackInfo ci) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
