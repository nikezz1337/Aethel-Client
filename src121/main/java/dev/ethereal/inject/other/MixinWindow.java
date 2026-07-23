package dev.ethereal.inject.other;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.other.FramebufferResizeEvent;
import dev.ethereal.api.event.events.other.WindowResizeEvent;
import dev.ethereal.paste.xweb.Profile;

@Mixin(Window.class)
public class MixinWindow {
    @Shadow @Final private long handle;

    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private String etherealTitleHook(String original) {
        return "Ethereal client (" + Profile.getUsername() + ")";
    }

    @Inject(method = "onWindowSizeChanged", at = @At("RETURN"))
    public void windowResizeHook(long window, int width, int height, CallbackInfo ci) {
        Events.post(new WindowResizeEvent());
    }

    @Inject(method = "onFramebufferSizeChanged", at = @At("RETURN"))
    public void framebufferResizeHook(long window, int width, int height, CallbackInfo callbackInfo) {
        if (window == handle) {
            Events.post(new FramebufferResizeEvent());
        }
    }
}
