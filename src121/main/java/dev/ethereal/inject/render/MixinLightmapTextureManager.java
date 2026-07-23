package dev.ethereal.inject.render;

import dev.ethereal.client.features.modules.render.FullbrightModule;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager {
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Uniform;set(F)V", ordinal = 0), index = 0)
    private float ethereal$adjustAmbientLight(float original) {
        FullbrightModule fullbright = getFullbright();
        return fullbright == null ? original : fullbright.adjustAmbientFactor(original);
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Uniform;set(F)V", ordinal = 1), index = 0)
    private float ethereal$adjustSkyLight(float original) {
        FullbrightModule fullbright = getFullbright();
        return fullbright == null ? original : fullbright.adjustSkyFactor(original);
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Uniform;set(F)V", ordinal = 2), index = 0)
    private float ethereal$adjustBlockLight(float original) {
        FullbrightModule fullbright = getFullbright();
        return fullbright == null ? original : fullbright.adjustBlockFactor(original);
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Uniform;set(F)V", ordinal = 4), index = 0)
    private float ethereal$adjustDarknessScale(float original) {
        FullbrightModule fullbright = getFullbright();
        return fullbright == null ? original : fullbright.adjustDarknessScale(original);
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Uniform;set(F)V", ordinal = 5), index = 0)
    private float ethereal$adjustDarkenWorldFactor(float original) {
        FullbrightModule fullbright = getFullbright();
        return fullbright == null ? original : fullbright.adjustDarkenWorldFactor(original);
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Uniform;set(F)V", ordinal = 6), index = 0)
    private float ethereal$adjustBrightnessFactor(float original) {
        FullbrightModule fullbright = getFullbright();
        return fullbright == null ? original : fullbright.adjustBrightnessFactor(original);
    }

    private static FullbrightModule getFullbright() {
        FullbrightModule fullbright = FullbrightModule.getInstance();
        return fullbright.isEnabled() ? fullbright : null;
    }
}
