package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.module.Module;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.rotation.RotationUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Holds a single rotation request — mirrors RotationComponent logic.
 * Supports three update modes:
 *   - plain (speed-clamped, no easing)
 *   - interp (easing on a single speed)
 *   - interpMinMax (easing between min and max speed — most human-like)
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class RotationPlan implements dev.ethereal.api.system.interfaces.QuickImports {

    public enum Task { AIM, RESET, IDLE }

    // ── request data ──────────────────────────────────────────────────────
    private final Rotation rotation;
    private final Vec3d vec3d;
    private final Entity entity;
    private final RotationMode rotationMode;
    private final int ticksUntilReset;
    private final float resetThreshold;
    private final boolean moveCorrection;
    private boolean freeMoveCorrection;
    private boolean clientLook;
    private final Module provider;

    // ── easing params (null = plain mode) ────────────────────────────────
    private final Easing easing;
    private final float easingStrength;
    private final float minYawSpeed;    // -1 = not used
    private final float minPitchSpeed;  // -1 = not used

    // ── per-plan interp state (mirrors interpYaw/interpPitch in RotationComponent) ──
    private float interpYaw   = Float.NaN;
    private float interpPitch = Float.NaN;

    // ── task state ────────────────────────────────────────────────────────
    private Task task = Task.IDLE;

    // ── full constructor ──────────────────────────────────────────────────
    public RotationPlan(Rotation rotation, Vec3d vec3d, Entity entity, RotationMode rotationMode,
                        int ticksUntilReset, float resetThreshold,
                        boolean moveCorrection, boolean freeMoveCorrection,
                        Easing easing, float easingStrength,
                        float minYawSpeed, float minPitchSpeed,
                        Module provider) {
        this.rotation         = rotation;
        this.vec3d            = vec3d;
        this.entity           = entity;
        this.rotationMode     = rotationMode;
        this.ticksUntilReset  = ticksUntilReset;
        this.resetThreshold   = resetThreshold;
        this.moveCorrection   = moveCorrection;
        this.freeMoveCorrection = freeMoveCorrection;
        this.easing           = easing;
        this.easingStrength   = easingStrength;
        this.minYawSpeed      = minYawSpeed;
        this.minPitchSpeed    = minPitchSpeed;
        this.provider         = provider;
        this.task             = Task.AIM;
    }

    /** Legacy constructor — no easing. */
    public RotationPlan(Rotation rotation, Vec3d vec3d, Entity entity, RotationMode rotationMode,
                        int ticksUntilReset, float resetThreshold,
                        boolean moveCorrection, boolean freeMoveCorrection,
                        Module provider) {
        this(rotation, vec3d, entity, rotationMode, ticksUntilReset, resetThreshold,
                moveCorrection, freeMoveCorrection, null, 0.3f, -1f, -1f, provider);
    }

    // ── interp seed ───────────────────────────────────────────────────────

    /** Called once on first tick to seed the interpolation state. */
    public void initInterp(Rotation from) {
        if (Float.isNaN(interpYaw)) {
            interpYaw   = from.getYaw();
            interpPitch = from.getPitch();
        }
    }

    // ── main dispatch ─────────────────────────────────────────────────────

    public Rotation nextRotation(Rotation fromRotation, boolean isResetting) {
        if (isResetting) {
            // smooth return — mirrors RotationComponent.resetRotation()
            return updateRotation(fromRotation,
                    RotationUtil.fromVec2f(mc.player.getRotationClient()),
                    45f, 30f);
        }

        // delegate to rotation mode first to get the target for this tick
        Rotation modeTarget = rotationMode.process(fromRotation, rotation, vec3d, entity);

        if (easing != null && minYawSpeed > 0 && minPitchSpeed > 0) {
            return updateRotationInterpMinMax(fromRotation, modeTarget,
                    minYawSpeed, Math.abs(MathHelper.wrapDegrees(modeTarget.getYaw()   - fromRotation.getYaw())),
                    minPitchSpeed, Math.abs(modeTarget.getPitch() - fromRotation.getPitch()),
                    easing, easingStrength);
        }

        if (easing != null) {
            return updateRotationInterp(fromRotation, modeTarget,
                    Math.abs(MathHelper.wrapDegrees(modeTarget.getYaw()   - fromRotation.getYaw())),
                    Math.abs(modeTarget.getPitch() - fromRotation.getPitch()),
                    easing, easingStrength);
        }

        return modeTarget;
    }

    // ── update variants (mirrors RotationComponent private methods) ───────

    /**
     * Plain speed-clamped update — no easing.
     * Mirrors: updateRotation(target, yawSpeed, pitchSpeed)
     */
    public static Rotation updateRotation(Rotation current, Rotation target, float yawSpeed, float pitchSpeed) {
        float yawDelta   = MathHelper.wrapDegrees(target.getYaw()   - current.getYaw());
        float pitchDelta = target.getPitch() - current.getPitch();

        float clampedYaw   = Math.min(Math.abs(yawDelta),   yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        float newYaw   = current.getYaw()   + MathHelper.clamp(yawDelta,   -clampedYaw,   clampedYaw);
        float newPitch = current.getPitch() + MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch);

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
    }

    /**
     * Easing interp on a single speed — mirrors updateRotationInterp / updateRotation(easing).
     * interpYaw/interpPitch are advanced each tick, then applied to player.
     */
    private Rotation updateRotationInterp(Rotation current, Rotation target,
                                          float yawSpeed, float pitchSpeed,
                                          Easing easing, float easingStrength) {
        initInterp(current);

        float targetYaw   = target.getYaw();
        float targetPitch = target.getPitch();

        float yawDelta   = MathHelper.wrapDegrees(targetYaw   - interpYaw);
        float pitchDelta = targetPitch - interpPitch;

        float yawDist   = Math.abs(yawDelta);
        float pitchDist = Math.abs(pitchDelta);

        float yawProgress   = Math.min(yawDist   / 180f, 1f);
        float pitchProgress = Math.min(pitchDist / 90f,  1f);

        float easedYaw   = (float) easing.apply((double) yawProgress);
        float easedPitch = (float) easing.apply((double) pitchProgress);

        float yawStep   = Math.min(yawDist,   yawSpeed   * Math.max(easedYaw,   0.01f));
        float pitchStep = Math.min(pitchDist, pitchSpeed * Math.max(easedPitch, 0.01f));

        interpYaw   += MathHelper.clamp(yawDelta,   -yawStep,   yawStep);
        interpPitch += MathHelper.clamp(pitchDelta, -pitchStep, pitchStep);

        interpYaw   = MathHelper.wrapDegrees(interpYaw);
        interpPitch = MathHelper.clamp(interpPitch, -90f, 90f);

        // apply interp delta on top of current player rotation
        float finalYawDelta   = MathHelper.wrapDegrees(interpYaw   - current.getYaw());
        float finalPitchDelta = interpPitch - current.getPitch();

        return new Rotation(
                current.getYaw()   + finalYawDelta,
                MathHelper.clamp(current.getPitch() + finalPitchDelta, -90f, 90f)
        );
    }

    /**
     * Min/max speed with easing wave — mirrors updateRotationInterpMinMax.
     * Speed oscillates between min and max based on angular progress + easing curve.
     */
    private Rotation updateRotationInterpMinMax(Rotation current, Rotation target,
                                                float minYaw, float maxYaw,
                                                float minPitch, float maxPitch,
                                                Easing easing, float easingStrength) {
        float yawDelta   = MathHelper.wrapDegrees(target.getYaw()   - current.getYaw());
        float pitchDelta = target.getPitch() - current.getPitch();

        float yawDist   = Math.abs(yawDelta);
        float pitchDist = Math.abs(pitchDelta);

        float yawProgress   = Math.min(yawDist   / 180f, 1f);
        float pitchProgress = Math.min(pitchDist / 90f,  1f);

        // wave: 0→1→0 as progress goes 0→0.5→1
        float yawWave   = (yawProgress   > 0.5f ? 1f - yawProgress   : yawProgress)   * 2f;
        float pitchWave = (pitchProgress > 0.5f ? 1f - pitchProgress : pitchProgress) * 2f;

        float yawSpeedPC, pitchSpeedPC;
        if (easing != null && easingStrength > 0f) {
            float easedYaw   = (float) easing.apply((double) yawWave);
            float easedPitch = (float) easing.apply((double) pitchWave);
            yawSpeedPC   = MathHelper.lerp(easingStrength, yawWave,   easedYaw);
            pitchSpeedPC = MathHelper.lerp(easingStrength, pitchWave, easedPitch);
        } else {
            yawSpeedPC   = yawWave;
            pitchSpeedPC = pitchWave;
        }

        float yawSpeed   = MathHelper.lerp(yawSpeedPC,   minYaw,   maxYaw);
        float pitchSpeed = MathHelper.lerp(pitchSpeedPC, minPitch, maxPitch);

        float yawStep   = MathHelper.clamp(yawDelta,   -yawSpeed,   yawSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        return new Rotation(
                MathHelper.wrapDegrees(current.getYaw()   + yawStep),
                MathHelper.clamp(current.getPitch() + pitchStep, -90f, 90f)
        );
    }
}
