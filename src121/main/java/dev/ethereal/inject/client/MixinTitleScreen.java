package dev.ethereal.inject.client;

import dev.ethereal.client.ui.mainmenu.Cherry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void ethereal$openCherry(CallbackInfo ci) {
        MinecraftClient.getInstance().setScreen(new Cherry());
        ci.cancel();
    }
}
