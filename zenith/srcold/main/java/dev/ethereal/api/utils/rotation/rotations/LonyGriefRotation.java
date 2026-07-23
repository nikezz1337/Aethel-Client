package dev.ethereal.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.rotation.misc.AuraUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationMode;

import java.util.Random;

public class LonyGriefRotation extends RotationMode {

    private static final Random rnd = new Random();

    // плавная интерполяция
    private float smoothYaw   = Float.NaN;
    private float smoothPitch = Float.NaN;

    // рваная скорость — яу и питч независимы
    private float yawSpeed      = 0.18f;
    private float pitchSpeed    = 0.15f;
    private int   yawSpeedTick  = 0;
    private int   nextYawChange = 3;
    private int   pitchSpeedTick  = 0;
    private int   nextPitchChange = 8; // питч меняется редко — плавность

    // overshoot
    private float overshootYaw   = 0f;
    private float overshootPitch = 0f;
    private float lastTargetYaw   = Float.NaN;
    private float lastTargetPitch = Float.NaN;

    // джерки — только яу
    private int   ticksSinceJerk = 0;
    private int   nextJerkTick   = 4;
    private float jerkYaw        = 0f;

    public LonyGriefRotation() {
        super("Lony Grief");
    }

    private float lerpAngle(float from, float to, float t) {
        return from + MathHelper.wrapDegrees(to - from) * t;
    }

    /** Нелинейная кривая — easeOut: быстро стартует, замедляется у цели */
    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    /** Нелинейная кривая — easeInOut: разгон + торможение */
    private float easeInOut(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
    }

    /** Имитация дрожания руки */
    private float microJitterYaw(long now) {
        double t = now / 1000.0;
        return (float)(
            Math.sin(t * 7.3  + 0.4) * 0.28 +
            Math.sin(t * 13.1 + 1.2) * 0.14 +
            Math.sin(t * 23.7 + 2.8) * 0.07 +
            (rnd.nextGaussian() * 0.06)
        );
    }

    private float microJitterPitch(long now) {
        double t = now / 1000.0;
        // питч — очень тихое дрожание, почти незаметное
        return (float)(
            Math.sin(t * 4.1 + 1.3) * 0.06 +
            Math.sin(t * 9.7 + 0.7) * 0.03 +
            (rnd.nextGaussian() * 0.02)
        );
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        if (entity == null) return current;
        if (vec3d == null) return current;

        long now = System.currentTimeMillis();

        if (Float.isNaN(smoothYaw)) {
            smoothYaw   = current.getYaw();
            smoothPitch = current.getPitch();
        }

        Rotation targetRot = RotationUtil.rotationAt(vec3d);
        float tYaw   = targetRot.getYaw();
        float tPitch = targetRot.getPitch();

        // ── OVERSHOOT при смене поинта ────────────────────────────────────
        if (!Float.isNaN(lastTargetYaw)) {
            float yawJump   = Math.abs(MathHelper.wrapDegrees(tYaw   - lastTargetYaw));
            float pitchJump = Math.abs(MathHelper.wrapDegrees(tPitch - lastTargetPitch));
            if (yawJump > 4f || pitchJump > 3f) {
                overshootYaw   = MathHelper.wrapDegrees(tYaw   - lastTargetYaw) * MathUtil.random(0.15f, 0.35f);
                // питч overshoot очень маленький
                overshootPitch = MathHelper.wrapDegrees(tPitch - lastTargetPitch) * MathUtil.random(0.05f, 0.12f);
            }
        }
        lastTargetYaw   = tYaw;
        lastTargetPitch = tPitch;

        overshootYaw   *= 0.58f;
        overshootPitch *= 0.70f; // питч overshoot затухает медленнее — плавнее

        // ── ЯУ — нелинейная рваная скорость ──────────────────────────────
        yawSpeedTick++;
        if (yawSpeedTick >= nextYawChange) {
            float[] yawSpeeds = { 0.07f, 0.13f, 0.25f, 0.42f, 0.60f, 0.16f, 0.33f, 0.70f, 0.50f, 0.09f };
            yawSpeed     = yawSpeeds[rnd.nextInt(yawSpeeds.length)] + MathUtil.random(-0.02f, 0.02f);
            yawSpeedTick = 0;
            nextYawChange = MathUtil.random(3, 8);
        }
        float yawDelta = MathHelper.wrapDegrees(tYaw + overshootYaw - smoothYaw);
        float yawCurve = rnd.nextFloat() < 0.5f ? easeOut(yawSpeed) : yawSpeed;
        smoothYaw += yawDelta * yawCurve;

        // ── ПИТЧ — плавный, медленно меняет скорость ─────────────────────
        pitchSpeedTick++;
        if (pitchSpeedTick >= nextPitchChange) {
            // узкий диапазон скоростей — нет резких скачков
            float[] pitchSpeeds = { 0.10f, 0.14f, 0.18f, 0.22f, 0.26f, 0.12f, 0.20f, 0.16f };
            pitchSpeed      = pitchSpeeds[rnd.nextInt(pitchSpeeds.length)] + MathUtil.random(-0.01f, 0.01f);
            pitchSpeedTick  = 0;
            nextPitchChange = MathUtil.random(6, 14); // редко меняется
        }
        float pitchDelta = MathHelper.wrapDegrees(tPitch + overshootPitch - smoothPitch);
        // всегда easeInOut — плавный старт и конец
        smoothPitch += pitchDelta * easeInOut(pitchSpeed);

        // ── ДЖЕРКИ — только яу ───────────────────────────────────────────
        ticksSinceJerk++;
        if (ticksSinceJerk >= nextJerkTick) {
            jerkYaw += MathUtil.random(-5f, 5f);
            ticksSinceJerk = 0;
            nextJerkTick   = MathUtil.random(4, 9);
        }
        jerkYaw *= 0.45f;

        // ── МИКРОДЖИТТЕР ──────────────────────────────────────────────────
        float mJitYaw   = microJitterYaw(now);
        float mJitPitch = microJitterPitch(now);

        float outYaw   = smoothYaw   + jerkYaw + mJitYaw;
        float outPitch = smoothPitch + mJitPitch; // питч — только тихий джиттер

        // ── GCD ───────────────────────────────────────────────────────────
        float gcd  = 0.00006f + rnd.nextFloat() * 0.00007f;
        float dYaw   = Math.round((outYaw   - current.getYaw())   / gcd) * gcd;
        float dPitch = Math.round((outPitch - current.getPitch()) / gcd) * gcd;

        float glide = mc.player.isGliding() ? 0.42f : 1.0f;
        dYaw   = MathHelper.clamp(dYaw,   -MathUtil.random(38f, 55f) * glide, MathUtil.random(38f, 55f) * glide);
        // питч — жёсткий лимит за тик, не даём прыгать
        dPitch = MathHelper.clamp(dPitch, -MathUtil.random(6f, 10f) * glide, MathUtil.random(6f, 10f) * glide);

        return new Rotation(
            current.getYaw()   + dYaw,
            MathHelper.clamp(current.getPitch() + dPitch, -89f, 89f)
        );
    }
}