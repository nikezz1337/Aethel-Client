package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.*;
import dev.ethereal.api.event.events.other.RotationUpdateEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.event.events.player.other.PostRotationMovementInputEvent;
import dev.ethereal.api.event.events.player.move.VelocityEvent;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.task.TaskPriority;
import dev.ethereal.api.utils.task.TaskProcessor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;

/**
 * Central rotation manager — mirrors RotationComponent from the reference client.
 *
 * Task lifecycle:
 *   IDLE  → addRotation() → AIM  (rotating toward target)
 *   AIM   → timeout       → RESET (smoothly returning to free-look)
 *   RESET → reached       → IDLE
 */
@Getter
@Setter
public class RotationManager implements QuickImports {
    @Getter private static final RotationManager instance = new RotationManager();

    // ── state ─────────────────────────────────────────────────────────────
    private RotationPlan.Task currentTask = RotationPlan.Task.IDLE;
    private RotationPlan lastRotationPlan;
    private final TaskProcessor<RotationPlan> rotationPlanRequestProcessor = new TaskProcessor<>();

    private Rotation currentRotation;
    private Rotation previousRotation;
    private Rotation serverRotation = Rotation.DEFAULT;

    // smooth-reset interp state (mirrors RotationComponent.resetRotation)
    private float resetInterpYaw   = Float.NaN;
    private float resetInterpPitch = Float.NaN;
    private float resetYawSpeed    = 45f;
    private float resetPitchSpeed  = 30f;

    // ── init ──────────────────────────────────────────────────────────────

    public void load() {
        VelocityEvent.getInstance().subscribe(new Listener<>(event -> {
            RotationPlan plan = getCurrentRotationPlan();
            if (plan != null && plan.moveCorrection()) {
                float yaw = plan.freeMoveCorrection() ? mc.player.getYaw() : getRotation().getYaw();
                event.setVelocity(Entity.movementInputToVelocity(event.getMovementInput(), event.getSpeed(), yaw));
            }
        }));

        PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isSend()) return;
            Rotation rotation;
            if (event.packet() instanceof PlayerMoveC2SPacket p && p.changesLook()) {
                rotation = new Rotation(p.getYaw(1f), p.getPitch(1f));
            } else if (event.packet() instanceof PlayerPositionLookS2CPacket p) {
                rotation = new Rotation(p.change().yaw(), p.change().pitch());
            } else return;
            if (!PacketEvent.getInstance().isCancel()) serverRotation = rotation;
        }));

        MovementInputEvent.getInstance().subscribe(new Listener<>(event ->
                PostRotationMovementInputEvent.getInstance().call()));

        GameLoopEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            RotationUpdateEvent.getInstance().call();
            tick();
        }));
    }

    // ── public API ────────────────────────────────────────────────────────

    public Rotation getRotation() {
        if (mc.player == null) return Rotation.DEFAULT;
        return currentRotation != null ? currentRotation : RotationUtil.fromVec2f(mc.player.getRotationClient());
    }

    public Rotation getPreviousRotation() {
        if (mc.player == null) return Rotation.DEFAULT;
        return previousRotation != null ? previousRotation : RotationUtil.fromVec2f(mc.player.getRotationClient());
    }

    public RotationPlan getCurrentRotationPlan() {
        RotationPlan active = rotationPlanRequestProcessor.fetchActiveTaskValue();
        return active != null ? active : lastRotationPlan;
    }

    /** True when actively rotating (AIM or RESET). */
    public boolean isRotating() {
        return currentTask != RotationPlan.Task.IDLE;
    }

    /** Force-stop all rotation and return to IDLE. */
    public void stopRotation() {
        currentTask = RotationPlan.Task.IDLE;
        resetInterpYaw   = Float.NaN;
        resetInterpPitch = Float.NaN;
        setRotation(null);
        lastRotationPlan = null;
    }

    /** Full reset — clears all state. */
    public void resetRotation() {
        if (currentRotation != null) {
            resetInterpYaw   = currentRotation.getYaw();
            resetInterpPitch = currentRotation.getPitch();
            currentTask = RotationPlan.Task.RESET;
        } else {
            currentTask = RotationPlan.Task.IDLE;
        }
        setRotation(null);
        lastRotationPlan = null;
        currentRotation  = null;
        previousRotation = null;
    }

    // ── addRotation overloads ─────────────────────────────────────────────

    public void addRotation(Rotation.VecRotation vecRotation, LivingEntity entity,
                            RotationStrategy config, TaskPriority priority, Module provider) {
        addRotation(config.createRotationPlan(vecRotation.rotation(), vecRotation.vec(), entity, provider),
                priority, provider);
    }

    public void addRotation(Rotation rotation, RotationStrategy config,
                            TaskPriority priority, Module provider) {
        addRotation(config.createRotationPlan(rotation, provider), priority, provider);
    }

    void addRotation(RotationPlan plan, TaskPriority priority, Module provider) {
        rotationPlanRequestProcessor.addTask(
                new TaskProcessor.Task<>(plan.ticksUntilReset(), priority.getPriority(), provider, plan));
        currentTask = RotationPlan.Task.AIM;
    }

    // ── internal ──────────────────────────────────────────────────────────

    private void setRotation(Rotation value) {
        previousRotation = (value == null)
                ? (currentRotation != null ? currentRotation
                : mc.player != null ? new Rotation(mc.player.getYaw(), mc.player.getPitch())
                : Rotation.DEFAULT)
                : currentRotation;
        currentRotation = value;
    }

    private void tick() {
        // ── RESET task: smoothly return to free-look ──────────────────────
        if (currentTask == RotationPlan.Task.RESET) {
            if (mc.player != null && !Float.isNaN(resetInterpYaw)) {
                Rotation from   = new Rotation(resetInterpYaw, resetInterpPitch);
                Rotation camRot = new Rotation(mc.player.getYaw(), mc.player.getPitch());
                Rotation next   = RotationPlan.updateRotation(from, camRot, resetYawSpeed, resetPitchSpeed)
                        .adjustSensitivity();
                resetInterpYaw   = next.getYaw();
                resetInterpPitch = next.getPitch();
                setRotation(next);
                if (next.getDelta(camRot) < 1f) {
                    stopRotation();
                }
            } else {
                stopRotation();
            }
            return;
        }

        // ── AIM task ──────────────────────────────────────────────────────
        RotationPlan activePlan = getCurrentRotationPlan();
        if (activePlan == null) return;

        Rotation playerRotation = RotationUtil.fromVec2f(mc.player.getRotationClient());
        activePlan.initInterp(currentRotation != null ? currentRotation : playerRotation);

        boolean isResetting = rotationPlanRequestProcessor.fetchActiveTaskValue() == null;

        if (isResetting) {
            double diff = computeRotationDifference(serverRotation, playerRotation);
            if (diff < activePlan.resetThreshold()) {
                if (currentRotation != null) {
                    mc.player.setYaw(currentRotation.getYaw()
                            + computeAngleDifference(mc.player.getYaw(), currentRotation.getYaw()));
                    mc.player.setPitch(currentRotation.getPitch()
                            + computeAngleDifference(mc.player.getPitch(), currentRotation.getPitch()));
                }
                // seed smooth reset
                if (currentRotation != null) {
                    resetInterpYaw   = currentRotation.getYaw();
                    resetInterpPitch = currentRotation.getPitch();
                    currentTask = RotationPlan.Task.RESET;
                } else {
                    currentTask = RotationPlan.Task.IDLE;
                }
                setRotation(null);
                lastRotationPlan = null;
                return;
            }
        }

        Rotation newRotation = activePlan.nextRotation(
                currentRotation != null ? currentRotation : playerRotation,
                isResetting
        ).adjustSensitivity();

        setRotation(newRotation);

        if (activePlan.clientLook()) {
            mc.player.setYaw(newRotation.getYaw());
            mc.player.setPitch(newRotation.getPitch());
        }

        lastRotationPlan = activePlan;
        rotationPlanRequestProcessor.tick(1);
    }

    private double computeRotationDifference(Rotation a, Rotation b) {
        return Math.hypot(
                MathHelper.abs(computeAngleDifference(a.getYaw(),   b.getYaw())),
                MathHelper.abs(computeAngleDifference(a.getPitch(), b.getPitch()))
        );
    }

    private float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }
}
