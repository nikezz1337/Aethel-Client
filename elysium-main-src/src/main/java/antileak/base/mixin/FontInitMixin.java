package antileak.base.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import antileak.base.api.utils.render.fonts.ttf.Fonts;

@Mixin(MinecraftClient.class)
public class FontInitMixin {
    
    @Inject(method = "onFinishedLoading", at = @At("TAIL"))
    private void onFinishedLoading(CallbackInfo ci) {
        Fonts.init();
        antileak.base.api.utils.render.fonts.msdf.Fonts.init();
    }
}