package dev.aethel.mixin;

import dev.aethel.module.list.render.Removals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventHUD;
import dev.aethel.module.list.render.Interface;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At(value = "RETURN"))
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        new EventHUD(context, tickCounter).post();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Interface module = Aethel.getInstance().getModuleStorage().get(Interface.class);
        if (module != null && module.isEnabled() && module.isWidgetEnabled("Бафы")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelNausea(DrawContext context, float distortion, CallbackInfo ci) {
        if (Aethel.getInstance().getModuleStorage().get(Removals.class).isEnabled("Плохие эффекты")) {
            ci.cancel();
        }
    }
}
