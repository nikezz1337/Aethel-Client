package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.module.list.render.Removals;
import dev.aethel.module.list.render.WorldTweaks;
import dev.aethel.util.render.providers.ColorProvider;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void aethel$getFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> cir) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Плохие эффекты")) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void aethel$applyFog(
            net.minecraft.client.render.Camera camera,
            BackgroundRenderer.FogType fogType,
            org.joml.Vector4f color,
            float viewDistance,
            boolean thickenFog,
            float tickDelta,
            CallbackInfoReturnable<Fog> cir
    ) {
        WorldTweaks tweaks = Aethel.getInstance().getModuleStorage().get(WorldTweaks.class);
        if (tweaks != null && tweaks.isFogEnabled()) {
            float fogDistance = Math.max(12.0f, tweaks.getFogDistance());
            float fogEnd = Math.min(viewDistance, fogDistance);
            float fogStart = Math.max(0.0f, fogEnd * 0.05f);

            int themeColor = ColorProvider.getThemeColor();
            float r = ColorProvider.red(themeColor) / 255f;
            float g = ColorProvider.green(themeColor) / 255f;
            float b = ColorProvider.blue(themeColor) / 255f;

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
