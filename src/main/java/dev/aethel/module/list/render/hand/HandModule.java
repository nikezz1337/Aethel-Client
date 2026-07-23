package dev.aethel.module.list.render.hand;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventAfterWorldRender;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.SliderSetting;

@ModuleInformation(
        moduleName = "Hands",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Свечение рук и предметов"
)
public class HandModule extends Module {

    public final ModeSetting method = new ModeSetting("Method", "Layered", "Layered", "Single");
    public final ModeSetting mode = new ModeSetting("Mode", "Smoke", "Smoke", "Pretty")
            .setVisible(() -> method.is("Layered"));

    public final SliderSetting intensity = new SliderSetting("Intensity", 0.85, 0.0, 2.0, 0.01);
    public final SliderSetting blurRadius = new SliderSetting("Blur", 2.5, 0.5, 6.0, 0.1);

    public final SliderSetting speed = new SliderSetting("Speed", 1.15, 0.1, 3.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting trailLength = new SliderSetting("Trail Length", 0.55, 0.05, 1.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting trailFade = new SliderSetting("Trail Fade", 0.84, 0.5, 0.98, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting trailSoftness = new SliderSetting("Trail Softness", 1.35, 0.3, 2.5, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting trailBlur = new SliderSetting("Trail Blur", 1.55, 0.3, 3.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting smokeAmount = new SliderSetting("Smoke", 0.55, 0.0, 0.8, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting handSoftness = new SliderSetting("Hand Softness", 1.3, 0.3, 3.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting handBlur = new SliderSetting("Hand Blur", 1.45, 0.3, 3.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting prettyGlow = new SliderSetting("Glow", 1.0, 0.0, 2.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting prettyHeight = new SliderSetting("Height", 0.12, 0.02, 0.3, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting prettyWind = new SliderSetting("Wind", 1.0, 0.0, 3.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting prettyWave = new SliderSetting("Wave", 1.0, 0.0, 2.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting prettySpeed = new SliderSetting("Pretty Speed", 1.5, 0.1, 4.0, 0.01)
            .setVisible(() -> method.is("Layered"));
    public final SliderSetting prettyScale = new SliderSetting("Pretty Scale", 1.0, 0.1, 3.0, 0.1)
            .setVisible(() -> method.is("Layered"));
    public final BooleanSetting useThemeColor = new BooleanSetting("Theme Color", true);
    public final BooleanSetting useItemColor = new BooleanSetting("Item Color", false);

    private float prevYaw, prevPitch, smoothedDeltaYaw, smoothedDeltaPitch;
    private float smoothedActivity;

    @Override
    public void onEnable() {
        super.onEnable();
        HandRenderer.getInstance().setEnabled(true);
    }

    @Override
    public void onDisable() {
        HandRenderer.getInstance().setEnabled(false);
        super.onDisable();
    }

    @Subscribe
    public void onAfterWorld(EventAfterWorldRender event) {
        HandRenderer.getInstance().onAfterWorld(this);
    }

    public boolean isLayered() {
        return method.is("Layered");
    }

    public boolean isSingle() {
        return method.is("Single");
    }

    public float getInterpolatedActivity(float tickDelta) {
        return smoothedActivity;
    }

    public float getInterpolatedDeltaYaw(float tickDelta) {
        return smoothedDeltaYaw;
    }

    public float getInterpolatedDeltaPitch(float tickDelta) {
        return smoothedDeltaPitch;
    }

    public void updateActivity(float swingProgress, boolean usePressed, float motionStrength) {
        boolean activitySwing = swingProgress > 0.01F;
        boolean activityUse = usePressed;
        boolean activityMotion = motionStrength > 0.05F;

        float target = 0.0F;
        if (activitySwing) target = Math.max(target, 0.8F);
        if (activityUse) target = Math.max(target, 0.6F);
        if (activityMotion) target = Math.max(target, 0.3F);
        smoothedActivity += (target - smoothedActivity) * 0.15F;
    }

    public void updateCameraDelta(float yaw, float pitch) {
        float deltaYaw = yaw - prevYaw;
        float deltaPitch = pitch - prevPitch;
        prevYaw = yaw;
        prevPitch = pitch;

        smoothedDeltaYaw += (deltaYaw - smoothedDeltaYaw) * 0.18F;
        smoothedDeltaPitch += (deltaPitch - smoothedDeltaPitch) * 0.18F;
    }
}
