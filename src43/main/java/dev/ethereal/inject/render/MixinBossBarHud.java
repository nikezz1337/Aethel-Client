package dev.ethereal.inject.render;

import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.features.modules.other.Streamer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.client.features.modules.render.RemovalsModule;
import dev.ethereal.client.ui.widget.WidgetManager;

@Mixin(BossBarHud.class)
public class MixinBossBarHud {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        
        boolean bossBarWidgetEnabled = WidgetManager.getInstance().getWidgets().stream()
                .anyMatch(w -> w.getName().equals("BossBar") && w.isEnabled());

        if (Streamer.getInstance().isEnabled() && PlayerUtil.isFT() && PlayerUtil.isFTRepl()) {

        }

        if (RemovalsModule.getInstance().isBossBar() || bossBarWidgetEnabled) {
            ci.cancel();
        }
    }
}
