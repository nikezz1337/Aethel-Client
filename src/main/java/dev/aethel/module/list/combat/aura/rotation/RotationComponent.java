package dev.aethel.module.list.combat.aura.rotation;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import net.minecraft.util.math.MathHelper;

public class RotationComponent extends Component {

    private static RotationComponent instance;

    public static RotationComponent getInstance() {
        if (instance == null) instance = new RotationComponent();
        return instance;
    }

    private RotationTask currentTask = RotationTask.IDLE;
    private float currentYawSpeed;
    private float currentPitchSpeed;
    private float currentYawReturnSpeed;
    private float currentPitchReturnSpeed;
    private int currentPriority;
    private int currentTimeout;
    private int idleTicks;
    private Rotation targetRotation;

    public RotationTask getCurrentTask() { return currentTask; }
    public void setCurrentTask(RotationTask task) { this.currentTask = task; }
    public float getCurrentYawSpeed() { return currentYawSpeed; }
    public void setCurrentYawSpeed(float s) { this.currentYawSpeed = s; }
    public float getCurrentPitchSpeed() { return currentPitchSpeed; }
    public void setCurrentPitchSpeed(float s) { this.currentPitchSpeed = s; }
    public float getCurrentYawReturnSpeed() { return currentYawReturnSpeed; }
    public void setCurrentYawReturnSpeed(float s) { this.currentYawReturnSpeed = s; }
    public float getCurrentPitchReturnSpeed() { return currentPitchReturnSpeed; }
    public void setCurrentPitchReturnSpeed(float s) { this.currentPitchReturnSpeed = s; }
    public int getCurrentPriority() { return currentPriority; }
    public RotationComponent setCurrentPriority(int p) { this.currentPriority = p; return this; }
    public int getCurrentTimeout() { return currentTimeout; }
    public void setCurrentTimeout(int t) { this.currentTimeout = t; }
    public int getIdleTicks() { return idleTicks; }
    public void setIdleTicks(int t) { this.idleTicks = t; }
    public Rotation getTargetRotation() { return targetRotation; }
    public void setTargetRotation(Rotation r) { this.targetRotation = r; }

    // Fluent methods
    public RotationTask currentTask() { return currentTask; }
    public void currentTask(RotationTask task) { this.currentTask = task; }
    public int currentPriority() { return currentPriority; }
    public RotationComponent currentPriority(int p) { this.currentPriority = p; return this; }
    public int idleTicks() { return idleTicks; }
    public void idleTicks(int t) { this.idleTicks = t; }
    public float currentYawReturnSpeed() { return currentYawReturnSpeed; }
    public float currentPitchReturnSpeed() { return currentPitchReturnSpeed; }
    public int currentTimeout() { return currentTimeout; }

    public static void resetParentTimeout() {
        final RotationComponent instance = RotationComponent.getInstance();
        instance.currentTimeout = 0;
        instance.currentTask = RotationTask.IDLE;
        instance.currentPriority = 0;
        FreeLookComponent.setActive(false);
    }

    private void resetRotation() {
        Rotation targetRotation = new Rotation(FreeLookComponent.getFreeYaw(), FreeLookComponent.getFreePitch());
        if (updateRotation(targetRotation, currentYawReturnSpeed, currentPitchReturnSpeed)) {
            stopRotation();
        }
    }

    @Subscribe
    public void onEvent(EventTick event) {
        if (currentTask.equals(RotationTask.AIM) && idleTicks > currentTimeout) {
            currentTask = RotationTask.RESET;
        }

        if (currentTask.equals(RotationTask.RESET)) {
            resetRotation();
        }
        idleTicks++;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null) return false;

        Rotation currentRotation = new Rotation(mc.player);
        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        mc.player.setYaw(mc.player.getYaw() + MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw));
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch), -90F, 90F));

        idleTicks = 0;
        return new Rotation(mc.player).getDelta(targetRotation) < 1F;
    }

    public void stopRotation() {
        currentTask = RotationTask.IDLE;
        currentPriority = 0;
        FreeLookComponent.setActive(false);
    }

    public boolean isRotating() {
        return !currentTask.equals(RotationTask.IDLE);
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}
