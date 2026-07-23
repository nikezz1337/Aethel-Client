package dev.ethereal.client.features.modules.combat.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.security.SecureRandom;

public class PolarStepSmoothMode extends AngleSmoothMode {
    private final SecureRandom random = new SecureRandom();
    private boolean attackReady;
    private long lastAttackTime = 0;

    public PolarStepSmoothMode() {
        super("PolarStep");
    }

    @Override
    public void onAttack() {
        this.attackReady = true;
        this.lastAttackTime = System.currentTimeMillis();
    }
    public void setAttackReady(boolean attackReady) {

        this.attackReady = attackReady;

    }
    @Override
    public void onMiss() {
        this.attackReady = false;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        if (attackReady && System.currentTimeMillis() - lastAttackTime > 100) {
            attackReady = false;
        }

        Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        float difference = (float) Math.hypot(yawDelta, pitchDelta);

        if (difference < 0.01F) return currentAngle;

        float speed = attackReady
                ? 0.86F + random.nextFloat() * 0.14F
                : 0.38F + random.nextFloat() * 0.22F;

        float lineYaw = Math.abs(yawDelta / difference) * (attackReady ? 220.0F : 120.0F);
        float linePitch = Math.abs(pitchDelta / difference) * (attackReady ? 180.0F : 90.0F);

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        return new Angle(
                MathHelper.lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw),
                MathHelper.clamp(MathHelper.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch), -90.0F, 90.0F)
        );
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
