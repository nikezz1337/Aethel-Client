package dev.ethereal.api.utils.rotation.smooth;

import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

import static dev.ethereal.api.system.interfaces.QuickImports.mc;

public class FunTimeSmoothMode extends AngleSmoothMode {
    private boolean canAttack = false;
    private StopWatch attackTimer = new StopWatch();
    private int count = 0;
    private long smoothbackShakeStartMs = -1L;

    public FunTimeSmoothMode() {
        super("FunTime");
    }

    public void updateState(boolean canAttack, StopWatch attackTimer, int count) {
        this.canAttack = canAttack;
        this.attackTimer = attackTimer;
        this.count = count;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {

        if (canAttack) {
            this.smoothbackShakeStartMs = -1L;

            Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
            float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
            float totalDelta = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

            float yawLimit   = Math.abs(yawDelta   / totalDelta) * 130.0F;
            float pitchLimit = Math.abs(pitchDelta / totalDelta) * 130.0F;

            return new Angle(
                    MathHelper.lerp(0.85F, currentAngle.getYaw(),
                            currentAngle.getYaw() + MathHelper.clamp(yawDelta, -yawLimit, yawLimit)),
                    MathHelper.lerp(0.85F, currentAngle.getPitch(),
                            currentAngle.getPitch() + MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit))
            );
        }

        Angle playerTurns = new Angle(mc.player.getYaw(), mc.player.getPitch());
        Angle returnDelta = AngleUtil.calculateDelta(currentAngle, playerTurns);

        float retYaw   = returnDelta.getYaw();
        float retPitch = returnDelta.getPitch();
        float retTotal = (float) Math.hypot(retYaw, retPitch);

        float shakeYaw   = (float) (randomBetween(18.0F, 28.0F)
                * Math.sin((double) System.currentTimeMillis() / 60.0));
        float shakePitch = (float) (randomBetween(6.0F, 16.0F)
                * Math.cos((double) System.currentTimeMillis() / 60.0));

            if (this.smoothbackShakeStartMs < 0L) {
                this.smoothbackShakeStartMs = System.currentTimeMillis();
            }

            float fadeRatio = 1.0F - MathHelper.clamp(
                    (float)(System.currentTimeMillis() - this.smoothbackShakeStartMs) / 1000.0F,
                    0.0F, 1.0F
            );
            shakeYaw   *= fadeRatio;
            shakePitch *= fadeRatio;

        float limitMultiplier = !attackTimer.finished(535.0) ? 0.0F : 45.0F;
        float yawLimit   = Math.abs(retYaw   / retTotal) * limitMultiplier;
        float pitchLimit = Math.abs(retPitch / retTotal) * limitMultiplier;

        if (count % 86 == 0 && count > 0
                && !attackTimer.finished(250.0)) {

            shakePitch = -90.0F;

            if (attackTimer.finished(240.0)) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
            Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(0.85F, currentAngle.getYaw(),
                    currentAngle.getYaw() + MathHelper.clamp(retYaw, -yawLimit, yawLimit) + shakeYaw));
            moveAngle.setPitch(MathHelper.lerp(0.85F, currentAngle.getPitch(),
                    currentAngle.getPitch() + MathHelper.clamp(retPitch, -pitchLimit, pitchLimit) + shakePitch));

            return moveAngle;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private float randomBetween(float min, float max) {
        if (min == max) return min;
        if (min > max) {
            float tmp = min;
            min = max;
            max = tmp;
        }
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }
}
