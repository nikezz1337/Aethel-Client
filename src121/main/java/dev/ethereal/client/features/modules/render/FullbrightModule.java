package dev.ethereal.client.features.modules.render;

import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.inject.accessors.LightmapTextureManagerAccessor;
import lombok.Getter;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

@ModuleRegister(name = "Fullbright", category = Category.RENDER)
public class FullbrightModule extends Module {
    @Getter private static final FullbrightModule instance = new FullbrightModule();

    private final SliderSetting strength = new SliderSetting("Strength")
            .value(0.78f).range(0.0f, 1.0f).step(0.01f)
            .onAction(this::markLightmapDirty);
    private final SliderSetting shadowLift = new SliderSetting("Shadow Lift")
            .value(0.42f).range(0.0f, 0.85f).step(0.01f)
            .onAction(this::markLightmapDirty);
    private final SliderSetting preserveLighting = new SliderSetting("Preserve Lighting")
            .value(0.82f).range(0.0f, 1.0f).step(0.01f)
            .onAction(this::markLightmapDirty);

    private double oldDarknessEffectScale = 1.0;

    public FullbrightModule() {
        addSettings(strength, shadowLift, preserveLighting);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.options != null) {
            oldDarknessEffectScale = mc.options.getDarknessEffectScale().getValue();
        }
        applyClientSettings();
        markLightmapDirty();
    }

    @Override
    public void onDisable() {
        restoreClientSettings();
        markLightmapDirty();
        super.onDisable();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        applyClientSettings();
        mc.player.removeStatusEffectInternal(StatusEffects.DARKNESS);
        mc.player.removeStatusEffectInternal(StatusEffects.BLINDNESS);
    }

    public float adjustAmbientFactor(float original) {
        float liftedAmbient = shadowLift.getValue() * 0.30f + strength.getValue() * 0.22f;
        float preservedAmbient = liftedAmbient * (1.0f - preserveLighting.getValue() * 0.42f);
        return MathHelper.clamp(Math.max(original, preservedAmbient), 0.0f, 0.65f);
    }

    public float adjustBlockFactor(float original) {
        float target = 1.55f
                + strength.getValue() * 0.45f
                + shadowLift.getValue() * 0.25f
                - preserveLighting.getValue() * 0.18f;
        return Math.max(original, target);
    }

    public float adjustSkyFactor(float original) {
        float lift = shadowLift.getValue() * 0.08f + strength.getValue() * 0.05f;
        return Math.max(original, original + lift);
    }

    public float adjustDarknessScale(float original) {
        return 0.0f;
    }

    public float adjustDarkenWorldFactor(float original) {
        float factor = 0.10f
                + preserveLighting.getValue() * 0.35f
                + (1.0f - strength.getValue()) * 0.18f
                + (1.0f - shadowLift.getValue()) * 0.10f;
        factor = MathHelper.clamp(factor, 0.16f, 0.72f);
        return original * factor;
    }

    public float adjustBrightnessFactor(float original) {
        float boosted = 0.62f
                + strength.getValue() * 0.28f
                + shadowLift.getValue() * 0.12f
                - preserveLighting.getValue() * 0.16f;
        return MathHelper.clamp(Math.max(original, boosted), 0.0f, 1.0f);
    }

    private void markLightmapDirty() {
        if (mc.gameRenderer == null) return;
        if (mc.gameRenderer.getLightmapTextureManager() instanceof LightmapTextureManagerAccessor accessor) {
            accessor.ethereal$setDirty(true);
        }
    }

    private void applyClientSettings() {
        if (mc.options == null) return;
        mc.options.getDarknessEffectScale().setValue(0.0);
    }

    private void restoreClientSettings() {
        if (mc.options == null) return;
        mc.options.getDarknessEffectScale().setValue(oldDarknessEffectScale);
    }
}
