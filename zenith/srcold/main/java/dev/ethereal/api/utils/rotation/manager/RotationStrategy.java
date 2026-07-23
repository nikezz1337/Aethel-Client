package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.module.Module;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.rotation.rotations.InstantRotation;
import dev.ethereal.api.utils.rotation.rotations.SmoothRotation;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Mirrors RotationComponent.update() overloads.
 * Build a strategy, then call createRotationPlan() to get a RotationPlan.
 */
@Setter
@Accessors(chain = true, fluent = true)
public class RotationStrategy {
    private final RotationMode rotationMode;
    private final boolean moveCorrection;
    private final boolean freeCorrection;

    private final float resetThreshold = 2f;
    private int ticksUntilReset = 5;
    private boolean clientLook;

    // easing params
    private Easing easing       = null;
    private float easingStrength = 0.3f;
    private float minYawSpeed   = -1f;
    private float minPitchSpeed = -1f;

    // ── presets ───────────────────────────────────────────────────────────
    public static final RotationStrategy SMOOTH_FREE  = new RotationStrategy(new SmoothRotation(), true, true);
    public static final RotationStrategy SMOOTH_FOCUS = new RotationStrategy(new SmoothRotation(), true);
    public static final RotationStrategy TARGET       = new RotationStrategy(new InstantRotation(), true);

    // ── constructors ──────────────────────────────────────────────────────
    public RotationStrategy(RotationMode rotationMode, boolean moveCorrection) {
        this.rotationMode  = rotationMode;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = false;
    }

    public RotationStrategy(RotationMode rotationMode, boolean moveCorrection, boolean freeCorrection) {
        this.rotationMode  = rotationMode;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = freeCorrection;
    }

    public RotationStrategy(boolean moveCorrection) {
        this(new SmoothRotation(), moveCorrection);
    }

    public RotationStrategy() {
        this(new SmoothRotation(), true, true);
    }

    // ── static update() mirrors — identical signatures to RotationComponent ──

    /**
     * Plain speed update — no easing.
     * Mirrors: update(target, yawSpeed, pitchSpeed, yawReturnSpeed, pitchReturnSpeed, timeout, priority, clientRotation)
     */
    public static void update(RotationManager manager, Rotation target, RotationMode mode,
                               float yawSpeed, float pitchSpeed,
                               float yawReturnSpeed, float pitchReturnSpeed,
                               int timeout, boolean moveCorrection, boolean freeCorrection,
                               boolean clientLook, Module provider) {
        RotationStrategy s = new RotationStrategy(mode, moveCorrection, freeCorrection)
                .ticksUntilReset(timeout)
                .clientLook(clientLook);
        manager.addRotation(s.createRotationPlan(target, null, null, provider),
                dev.ethereal.api.utils.task.TaskPriority.HIGH, provider);
    }

    /**
     * Easing interp update — single speed + easing.
     * Mirrors: update(target, yawSpeed, pitchSpeed, ..., typeInterp)
     */
    public static void update(RotationManager manager, Rotation target, RotationMode mode,
                               float yawSpeed, float pitchSpeed,
                               float yawReturnSpeed, float pitchReturnSpeed,
                               int timeout, boolean moveCorrection, boolean freeCorrection,
                               boolean clientLook, Easing typeInterp, float easingStrength,
                               Module provider) {
        RotationStrategy s = new RotationStrategy(mode, moveCorrection, freeCorrection)
                .ticksUntilReset(timeout)
                .clientLook(clientLook)
                .easing(typeInterp)
                .easingStrength(easingStrength);
        manager.addRotation(s.createRotationPlan(target, null, null, provider),
                dev.ethereal.api.utils.task.TaskPriority.HIGH, provider);
    }

    /**
     * Min/max speed with easing — most human-like.
     * Mirrors: update(target, minYaw, maxYaw, minPitch, maxPitch, ..., typeInterp, easingStrength)
     */
    public static void update(RotationManager manager, Rotation target, RotationMode mode,
                               float minYawSpeed, float maxYawSpeed,
                               float minPitchSpeed, float maxPitchSpeed,
                               float yawReturnSpeed, float pitchReturnSpeed,
                               int timeout, boolean moveCorrection, boolean freeCorrection,
                               boolean clientLook, Easing typeInterp, float easingStrength,
                               Module provider) {
        RotationStrategy s = new RotationStrategy(mode, moveCorrection, freeCorrection)
                .ticksUntilReset(timeout)
                .clientLook(clientLook)
                .easing(typeInterp)
                .easingStrength(easingStrength)
                .minYawSpeed(minYawSpeed)
                .minPitchSpeed(minPitchSpeed);
        manager.addRotation(s.createRotationPlan(target, null, null, provider),
                dev.ethereal.api.utils.task.TaskPriority.HIGH, provider);
    }

    /**
     * Shorthand — single turnSpeed + returnSpeed.
     * Mirrors: update(target, turnSpeed, returnSpeed, timeout, priority)
     */
    public static void update(RotationManager manager, Rotation target, RotationMode mode,
                               float turnSpeed, float returnSpeed, int timeout,
                               boolean moveCorrection, Module provider) {
        update(manager, target, mode, turnSpeed, turnSpeed, returnSpeed, returnSpeed,
                timeout, moveCorrection, false, false, provider);
    }

    // ── plan factory ──────────────────────────────────────────────────────

    public RotationPlan createRotationPlan(Rotation rotation, Vec3d vec, Entity entity, Module provider) {
        return new RotationPlan(rotation, vec, entity, rotationMode, ticksUntilReset, resetThreshold,
                moveCorrection, freeCorrection, easing, easingStrength, minYawSpeed, minPitchSpeed, provider)
                .clientLook(clientLook);
    }

    public RotationPlan createRotationPlan(Rotation rotation, Module provider) {
        return new RotationPlan(rotation, null, null, rotationMode, ticksUntilReset, resetThreshold,
                moveCorrection, freeCorrection, easing, easingStrength, minYawSpeed, minPitchSpeed, provider)
                .clientLook(clientLook);
    }

    public RotationPlan createRotationPlan(Rotation rotation, Vec3d vec, Entity entity,
                                           boolean moveCorrection, boolean freeCorrection, Module provider) {
        return new RotationPlan(rotation, vec, entity, rotationMode, ticksUntilReset, resetThreshold,
                moveCorrection, freeCorrection, easing, easingStrength, minYawSpeed, minPitchSpeed, provider)
                .clientLook(clientLook);
    }
}
