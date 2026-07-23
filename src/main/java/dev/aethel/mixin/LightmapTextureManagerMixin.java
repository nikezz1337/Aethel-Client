package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.module.list.render.FullBright;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    @Redirect(
            method = "update(F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;")
    )
    private Object aethel$fullbright(SimpleOption<Double> option) {
        FullBright fullBright = Aethel.getInstance().getModuleStorage().get(FullBright.class);
        if (fullBright != null && fullBright.isEnabled()) {
            return Math.max(option.getValue(), 1000.0);
        }
        return option.getValue();
    }
}
