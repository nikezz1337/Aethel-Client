package dev.ethereal.client.features.modules.render.targetesp;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.client.features.modules.combat.Aura;

public abstract class TargetEspMode implements QuickImports {
    public static final AnimationUtil showAnimation = new AnimationUtil();
    public static final AnimationUtil sizeAnimation = new AnimationUtil();
    public static LivingEntity currentTarget = null;
    public float prevShowAnimation = 0f;
    public float prevSizeAnimation = 0f;

    public Aura aura() {
        return Aura.getInstance();
    }

    public void updateTarget() {
        if (aura().getTarget() != null) {
            currentTarget = aura().getTarget();
        }
    }

    public void updateAnimation() {
        prevShowAnimation = (float) showAnimation.getValue();
        prevSizeAnimation = (float) sizeAnimation.getValue();

        boolean active = reason();

        long showDuration = active ? 300L : 120L;
        long sizeDuration = active ? 300L : 120L;

        showAnimation.update();
        showAnimation.run(active ? 1.0 : 0.0, showDuration, Easing.SINE_OUT);

        sizeAnimation.update();
        sizeAnimation.run(active ? 1.0 : 0.0, sizeDuration, Easing.SINE_OUT);
    }

    public boolean reason() {
        return aura().isEnabled() && aura().getTarget() != null;
    }

    public boolean canDraw() {
        if (mc.player == null || mc.world == null) return false;
        return showAnimation.getValue() > 0.0;
    }

    public static void updatePositions() {
        if (currentTarget != null && showAnimation.getValue() > 0.0) {
            lastTargetX = MathUtil.interpolate((float) currentTarget.prevX, (float) currentTarget.getX());
            lastTargetY = MathUtil.interpolate((float) currentTarget.prevY, (float) currentTarget.getY());
            lastTargetZ = MathUtil.interpolate((float) currentTarget.prevZ, (float) currentTarget.getZ());
        }

        targetX = lastTargetX;
        targetY = lastTargetY;
        targetZ = lastTargetZ;
    }

    @Getter private static double targetX = -1;
    @Getter private static double targetY = -1;
    @Getter private static double targetZ = -1;

    private static double lastTargetX = -1;
    private static double lastTargetY = -1;
    private static double lastTargetZ = -1;

    public abstract void onUpdate();
    public abstract void onRender3D(Render3DEvent event);
}
