package dev.ethereal.inject.input;

import dev.ethereal.Ethereal;
import dev.ethereal.api.event.events.player.other.LookEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Smoother;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.api.system.draggable.DraggableManager;
import dev.ethereal.client.ui.clickgui.ScreenClickGUI;

@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(method = "lockCursor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"), cancellable = true)
    private void lockCursorHook(CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ScreenClickGUI) {
            ci.cancel();
        }
    }

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;
    @Final @Shadow private Smoother cursorXSmoother;
    @Final @Shadow private Smoother cursorYSmoother;

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        if (client.player == null) return;

        double sensitivity = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double scaled = sensitivity * sensitivity * sensitivity * 8.0;
        double i, j;

        if (client.options.smoothCameraEnabled) {
            i = cursorXSmoother.smooth(cursorDeltaX * scaled, timeDelta * scaled);
            j = cursorYSmoother.smooth(cursorDeltaY * scaled, timeDelta * scaled);
        } else if (client.options.getPerspective().isFirstPerson() && client.player.isUsingSpyglass()) {
            cursorXSmoother.clear();
            cursorYSmoother.clear();
            i = cursorDeltaX * sensitivity * sensitivity * sensitivity;
            j = cursorDeltaY * sensitivity * sensitivity * sensitivity;
        } else {
            cursorXSmoother.clear();
            cursorYSmoother.clear();
            i = cursorDeltaX * scaled;
            j = cursorDeltaY * scaled;
        }

        double invert = client.options.getInvertYMouse().getValue() ? -1 : 1;

        i = MathHelper.clamp(i, -180, 180);
        j = MathHelper.clamp(j, -90, 90);

        LookEvent event = new LookEvent(i, j * invert);
        Events.post(event);

        if (!event.isCancelled()) {
            client.getTutorialManager().onUpdateMouse(event.getYaw(), event.getPitch());
            client.player.changeLookDirection(event.getYaw(), event.getPitch());
        }

        cursorDeltaX = 0.0;
        cursorDeltaY = 0.0;

        ci.cancel();
    }

    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/InactivityFpsLimiter;onInput()V"))
    public void mousePressHook(long window, int button, int action, int mods, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        Events.post(new KeyEvent(button, action, true));

        DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
            if (draggable.getModule().isEnabled()) {
                if (action == 0) {
                    draggable.onRelease(button);
                } else if (action == 1) {
                    draggable.onClick(button);
                }
            }
        });
    }
}
