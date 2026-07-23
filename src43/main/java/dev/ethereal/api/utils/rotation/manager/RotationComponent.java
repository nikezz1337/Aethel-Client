package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.math.MouseUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.math.MathHelper;

@Getter
@Setter
@Accessors(fluent = true)
public class RotationComponent implements QuickImports {
    @Getter private static final RotationComponent instance = new RotationComponent();

    public static RotationComponent getInstance() {
        return instance;
    }

    private Rotation currentRotation = null;
    private RotationTask currentTask = RotationTask.IDLE;
    private float currentYawSpeed;
    private float currentPitchSpeed;
    private float currentYawReturnSpeed;
    private float currentPitchReturnSpeed;
    private int currentPriority;
    private int currentTimeout;
    private int idleTicks;
    private int resetDelayTicks;
    private Rotation targetRotation;
    private float interpYaw;
    private float interpPitch;

    private float prevRealYaw = 0f;

    private RotationComponent() {
        UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            tick();
        }));
    }

    public boolean isRotating() {
        return currentTask != RotationTask.IDLE;
    }

    public void tick() {
        if (mc.player == null) return;

        if (currentTask == RotationTask.AIM && idleTicks > currentTimeout + 1) {
            currentTask = RotationTask.RESET;
            resetDelayTicks = 0;
        }

        if (currentTask == RotationTask.RESET) {
            if (resetDelayTicks > 0) {
                resetDelayTicks--;
                return;
            }
            resetRotation();
        }
        idleTicks++;
    }

    private void resetRotation() {
        if (mc.player == null) return;
        Rotation target = new Rotation(mc.player.getYaw(), mc.player.getPitch());

        if (updateRotationReset(target, currentYawReturnSpeed, currentPitchReturnSpeed)) {
            stopRotation();
        }
    }

    private boolean updateRotationReset(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null) return false;

        Rotation currentRot = currentRotation != null ? currentRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());
        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRot.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRot.getPitch();

        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        float nextYaw = currentRot.getYaw() + getSens(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw));
        float nextPitch = MathHelper.clamp(currentRot.getPitch() + getSens(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90F, 90F);

        currentRotation = new Rotation(nextYaw, nextPitch);
        return currentRotation.getDelta(targetRotation) < 1F;
    }

    private boolean updateRotationResetSmooth(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null) return false;

        Rotation currentRot = currentRotation != null ? currentRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());
        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRot.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRot.getPitch();

        float yawDist = Math.abs(yawDelta);
        float pitchDist = Math.abs(pitchDelta);

        // Прогресс от 0 до 1 (чем ближе к цели, тем медленнее)
        float yawProgress = Math.min(yawDist / 180.0F, 1.0F);
        float pitchProgress = Math.min(pitchDist / 90.0F, 1.0F);

        // Easing OUT_CUBIC для плавного замедления к концу
        float easedYaw = Easing.CUBIC_OUT.apply(yawProgress);
        float easedPitch = Easing.CUBIC_OUT.apply(pitchProgress);

        // Скорость уменьшается по мере приближения к цели
        float effectiveYawSpeed = Math.max(yawSpeed * easedYaw, 1.5f); // минимум 1.5 градуса
        float effectivePitchSpeed = Math.max(pitchSpeed * easedPitch, 1.2f); // минимум 1.2 градуса

        float clampedYaw = Math.min(yawDist, effectiveYawSpeed);
        float clampedPitch = Math.min(pitchDist, effectivePitchSpeed);

        float nextYaw = currentRot.getYaw() + getSens(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw));
        float nextPitch = MathHelper.clamp(currentRot.getPitch() + getSens(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90F, 90F);

        currentRotation = new Rotation(nextYaw, nextPitch);

        // Завершаем когда очень близко к цели (мягкий порог)
        return yawDist < 0.5F && pitchDist < 0.5F;
    }

    public void stopRotation() {
        currentTask = RotationTask.IDLE;
        currentPriority = 0;
        currentRotation = null;
        resetDelayTicks = 0;
    }

    public void startSmoothReset(int delayTicks) {
        if (mc.player == null || currentRotation == null) {
            stopRotation();
            return;
        }

        currentTask = RotationTask.RESET;
        currentPriority = 0;
        currentTimeout = 0;
        idleTicks = 0;
        resetDelayTicks = Math.max(delayTicks, 0);
    }

    public static float getSens(float delta) {
        float gcd = MouseUtil.getGCD();
        return Math.round(delta / gcd) * gcd;
    }

    public float getPrevRealYaw() {
        return prevRealYaw;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        final RotationComponent instance = RotationComponent.getInstance();
        if (instance.currentPriority() > priority) {
            return;
        }

        if (mc.player != null) {
            instance.prevRealYaw = mc.player.getYaw();
        }

        instance.currentYawSpeed(yawSpeed);
        instance.currentPitchSpeed(pitchSpeed);
        instance.currentYawReturnSpeed(yawReturnSpeed);
        instance.currentPitchReturnSpeed(pitchReturnSpeed);
        instance.currentTimeout(timeout);
        instance.currentPriority(priority);
        instance.currentTask(RotationTask.AIM);
        instance.resetDelayTicks(0);
        instance.targetRotation(target);

        instance.updateRotation(target, yawSpeed, pitchSpeed);

        if (clientRotation && mc.player != null && instance.currentRotation != null) {
            mc.player.setYaw(instance.currentRotation.getYaw());
            mc.player.setPitch(instance.currentRotation.getPitch());
        }
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation, Easing typeInterp) {
        final RotationComponent instance = RotationComponent.getInstance();
        if (instance.currentPriority() > priority) {
            return;
        }

        if (mc.player != null && instance.currentTask() == RotationTask.IDLE) {
            instance.interpYaw = mc.player.getYaw();
            instance.interpPitch = mc.player.getPitch();
        }

        if (mc.player != null) {
            instance.prevRealYaw = mc.player.getYaw();
        }

        instance.currentYawSpeed(yawSpeed);
        instance.currentPitchSpeed(pitchSpeed);
        instance.currentYawReturnSpeed(yawReturnSpeed);
        instance.currentPitchReturnSpeed(pitchReturnSpeed);
        instance.currentTimeout(timeout);
        instance.currentPriority(priority);
        instance.currentTask(RotationTask.AIM);
        instance.resetDelayTicks(0);
        instance.targetRotation(target);

        instance.updateRotationInterp(target, yawSpeed, pitchSpeed, typeInterp, 0.3f);

        if (clientRotation && mc.player != null && instance.currentRotation != null) {
            mc.player.setYaw(instance.currentRotation.getYaw());
            mc.player.setPitch(instance.currentRotation.getPitch());
        }
    }

    public static void update(Rotation target, float minYawSpeed, float maxYawSpeed, float minPitchSpeed, float maxPitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation, Easing typeInterp, float easingStrength) {
        final RotationComponent instance = RotationComponent.getInstance();
        if (instance.currentPriority() > priority) {
            return;
        }

        if (mc.player != null && instance.currentTask() == RotationTask.IDLE) {
            instance.interpYaw = mc.player.getYaw();
            instance.interpPitch = mc.player.getPitch();
        }

        if (mc.player != null) {
            instance.prevRealYaw = mc.player.getYaw();
        }

        instance.currentYawSpeed(maxYawSpeed);
        instance.currentPitchSpeed(maxPitchSpeed);
        instance.currentYawReturnSpeed(yawReturnSpeed);
        instance.currentPitchReturnSpeed(pitchReturnSpeed);
        instance.currentTimeout(timeout);
        instance.currentPriority(priority);
        instance.currentTask(RotationTask.AIM);
        instance.resetDelayTicks(0);
        instance.targetRotation(target);

        instance.updateRotationInterpMinMax(target, minYawSpeed, maxYawSpeed, minPitchSpeed, maxPitchSpeed, typeInterp, easingStrength);

        if (clientRotation && mc.player != null && instance.currentRotation != null) {
            mc.player.setYaw(instance.currentRotation.getYaw());
            mc.player.setPitch(instance.currentRotation.getPitch());
        }
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null) return false;

        Rotation currentRot = currentRotation != null ? currentRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());
        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRot.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRot.getPitch();

        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        float nextYaw = currentRot.getYaw() + getSens(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw));
        float nextPitch = MathHelper.clamp(currentRot.getPitch() + getSens(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90F, 90F);

        currentRotation = new Rotation(nextYaw, nextPitch);
        idleTicks = 0;
        return currentRotation.getDelta(targetRotation) < 1F;
    }

    private boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed, Easing easing, float easingStrength) {
        if (mc.player == null) return false;

        float targetYaw = targetRotation.getYaw();
        float targetPitch = targetRotation.getPitch();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - interpYaw);
        float pitchDelta = targetPitch - interpPitch;

        float yawDist = Math.abs(yawDelta);
        float pitchDist = Math.abs(pitchDelta);

        float yawProgress = Math.min(yawDist / 180.0F, 1.0F);
        float pitchProgress = Math.min(pitchDist / 90.0F, 1.0F);

        float easedYaw = easing.apply(yawProgress);
        float easedPitch = easing.apply(pitchProgress);

        float yawStep = Math.min(yawDist, yawSpeed * Math.max(easedYaw, 0.01F));
        float pitchStep = Math.min(pitchDist, pitchSpeed * Math.max(easedPitch, 0.01F));

        interpYaw += MathHelper.clamp(yawDelta, -yawStep, yawStep);
        interpPitch += MathHelper.clamp(pitchDelta, -pitchStep, pitchStep);

        interpYaw = MathHelper.wrapDegrees(interpYaw);
        interpPitch = MathHelper.clamp(interpPitch, -90F, 90F);

        Rotation currentRot = currentRotation != null ? currentRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float finalYawDelta = MathHelper.wrapDegrees(interpYaw - currentRot.getYaw());
        float finalPitchDelta = interpPitch - currentRot.getPitch();

        float nextYaw = currentRot.getYaw() + getSens(finalYawDelta);
        float nextPitch = MathHelper.clamp(currentRot.getPitch() + getSens(finalPitchDelta), -90F, 90F);

        currentRotation = new Rotation(nextYaw, nextPitch);

        idleTicks = 0;
        return yawDist < 1F && pitchDist < 1F;
    }

    private boolean updateRotationInterpMinMax(Rotation targetRotation, float minYawSpeed, float maxYawSpeed, float minPitchSpeed, float maxPitchSpeed, Easing easing, float easingStrength) {
        if (mc.player == null) return false;

        float targetYaw = targetRotation.getYaw();
        float targetPitch = targetRotation.getPitch();

        Rotation currentRot = currentRotation != null ? currentRotation : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentRot.getYaw());
        float pitchDelta = targetPitch - currentRot.getPitch();

        float yawDist = Math.abs(yawDelta);
        float pitchDist = Math.abs(pitchDelta);

        float yawProgress = Math.min(yawDist / 180.0F, 1.0F);
        float pitchProgress = Math.min(pitchDist / 90.0F, 1.0F);

        float yawProgressWave = (yawProgress > 0.5F ? 1.0F - yawProgress : yawProgress) * 2.0F;
        float pitchProgressWave = (pitchProgress > 0.5F ? 1.0F - pitchProgress : pitchProgress) * 2.0F;

        float yawSpeedPC = yawProgressWave;
        float pitchSpeedPC = pitchProgressWave;

        if (easing != null && easingStrength > 0.0F) {
            float easedYawProgress = easing.apply(yawProgressWave);
            float easedPitchProgress = easing.apply(pitchProgressWave);
            yawSpeedPC = MathHelper.lerp(easingStrength, yawProgressWave, easedYawProgress);
            pitchSpeedPC = MathHelper.lerp(easingStrength, pitchProgressWave, easedPitchProgress);
        }

        float yawSpeed = MathHelper.lerp(yawSpeedPC, minYawSpeed, maxYawSpeed);
        float pitchSpeed = MathHelper.lerp(pitchSpeedPC, minPitchSpeed, maxPitchSpeed);

        float yawStep = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        float nextYaw = MathHelper.wrapDegrees(currentRot.getYaw() + getSens(yawStep));
        float nextPitch = MathHelper.clamp(currentRot.getPitch() + getSens(pitchStep), -90F, 90F);

        currentRotation = new Rotation(nextYaw, nextPitch);

        idleTicks = 0;
        return yawDist < 1F && pitchDist < 1F;
    }

    private boolean updateRotationInterp(Rotation targetRotation, float yawSpeed, float pitchSpeed, Easing easing, float easingStrength) {
        return updateRotation(targetRotation, yawSpeed, pitchSpeed, easing, easingStrength);
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}
