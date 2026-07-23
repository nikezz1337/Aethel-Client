package dev.ethereal.inject.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ethereal.api.utils.color.UIColors;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ethereal.client.features.modules.render.AmbienceModule;
import dev.ethereal.client.features.modules.render.RemovalsModule;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
        if (RemovalsModule.getInstance().isBadEffects()) {
            info.setReturnValue(null);
        }
    }

@Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
private static void okak(Camera camera, BackgroundRenderer.FogType fogType, org.joml.Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
    AmbienceModule tweaks = AmbienceModule.getInstance();
    if (tweaks.isEnabled() && tweaks.customFog.getValue()) {
        float fogDistance = Math.max(12.0f, tweaks.fogDistance.getValue());
        float fogEnd = Math.min(viewDistance, fogDistance);
        float fogStart = Math.max(0.0f, fogEnd * 0.05f);

        java.awt.Color themeColor = UIColors.primary();
        float r = themeColor.getRed() / 255f;
        float g = themeColor.getGreen() / 255f;
        float b = themeColor.getBlue() / 255f;

        cir.setReturnValue(new Fog(
                fogStart,
                fogEnd,
                FogShape.SPHERE,
                r, g, b,
                1.0f
        ));
    }
  }
 }
