package dev.aethel.module.list.combat.aura.rotation;

import com.google.common.eventbus.Subscribe;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventTick;
import dev.aethel.event.list.MoveInputEvent;
import dev.aethel.module.list.combat.Aura;
import dev.aethel.module.list.combat.aura.UAttack;
import dev.aethel.module.list.combat.aura.util.GCDUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class URotations extends Component {

    public static RotationState rotationState = RotationState.IDLE;
    public static float aimYawSpeed;
    public static float aimPitchSpeed;
    public static float returnYawSpeed;
    public static float returnPitchSpeed;
    public static int rotationPriority;
    public static int rotationTimeout;
    public static int inactiveTicks;
    public static Rotation desiredRotation;
    private static float movementYaw;

    public static void resetParentTimeout() {
        rotationTimeout = 0;
        rotationState = RotationState.IDLE;
        rotationPriority = 0;
        FreeLookComponent.setActive(false);
    }
    

    public URotations() {
        Aethel.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onEvent(MoveInputEvent event) {
        if (mc.player == null || !rotationState.equals(RotationState.AIM)) return;

        Aura aura = Aura.getInstance();
        if (aura == null || Aura.target == null) return;

        if (UAttack.resetSprintTick(Aura.target, aura.ranges())) {
            event.forward = 0;
            event.strafe = 0;
            return;
        }

        String mode = aura.motion().getValue();

        if (mode.equals("Преследование")) {
            chaseTarget(event);
        } else if (mode.equals("Свободная")) {
            freeFixMovement(event);
        } else {
            fixMovement(event, mc.player.getYaw());
        }
    }

    private static void chaseTarget(MoveInputEvent event) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        if (forward == 0 && strafe == 0) return;

        LivingEntity target = Aura.target;
        Box box = target.getBoundingBox();

        Aura aura = Aura.getInstance();
        boolean goBehind = aura != null && aura.behindTarget.getValue();

        double tx, tz;
        if (goBehind) {
            double targetYawRad = Math.toRadians(target.getYaw());
            tx = target.getX() + Math.sin(targetYawRad) * 1.0;
            tz = target.getZ() - Math.cos(targetYawRad) * 1.0;
        } else {
            tx = (box.minX + box.maxX) / 2.0;
            tz = (box.minZ + box.maxZ) / 2.0;
        }

        double dx = tx - mc.player.getX();
        double dz = tz - mc.player.getZ();

        double dist = Math.sqrt(dx * dx + dz * dz);

        double angleToHitbox = MathHelper.wrapDegrees(
                Math.toDegrees(Math.atan2(dz, dx)) - 90.0);

        double inputAngle = Math.toDegrees(Math.atan2(-strafe, forward));
        float intendedAngle = MathHelper.wrapDegrees((float) (angleToHitbox + inputAngle));

        float sentYaw = desiredRotation != null ? desiredRotation.getYaw() : (float) angleToHitbox;

        float bestForward = 0, bestStrafe = 0;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward++) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe++) {
                if (testForward == 0 && testStrafe == 0) continue;
                double testAngle = MathHelper.wrapDegrees(
                        Math.toDegrees(direction(sentYaw, testForward, testStrafe)));
                float difference = Math.abs(MathHelper.wrapDegrees(
                        (float) (intendedAngle - testAngle)));
                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
        }

        event.forward = bestForward;
        event.strafe = bestStrafe;
    }

    private static double direction(float yaw, float moveForward, float moveStrafing) {
        if (moveForward < 0F) yaw += 180F;
        float magnitude = moveForward == 0 ? 1F : 0.5F;
        if (moveStrafing > 0F) yaw -= 90F * magnitude;
        if (moveStrafing < 0F) yaw += 90F * magnitude;
        return Math.toRadians(yaw);
    }

    private static void fixMovement(MoveInputEvent event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        if (forward == 0 && strafe == 0) return;

        double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, forward, strafe)));

        float bestForward = 0, bestStrafe = 0;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward++) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe++) {
                if (testForward == 0 && testStrafe == 0) continue;
                double testAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, testForward, testStrafe)));
                float difference = Math.abs(MathHelper.wrapDegrees((float) (targetAngle - testAngle)));
                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
        }

        event.forward = bestForward;
        event.strafe = bestStrafe;
    }

    /** Свободная коррекция (из wonderful-1.21.4): желаемое направление от freeYaw (взгляд), подбор WASD от bodyYaw (тело). */
    private static void freeFixMovement(MoveInputEvent event) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        double angle = MathHelper.wrapDegrees(Math.toDegrees(
                directionFree(FreeLookComponent.getFreeYaw(), forward, strafe)));
        if (forward != 0.0F || strafe != 0.0F) {
            float closestForward = 0.0F;
            float closestStrafe = 0.0F;
            float closestDifference = Float.MAX_VALUE;

            for (float predictedForward = -1.0F; predictedForward <= 1.0F; ++predictedForward) {
                for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; ++predictedStrafe) {
                    if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                        double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(
                                directionFree(mc.player.getYaw(), predictedForward, predictedStrafe)));
                        double difference = Math.abs(angle - predictedAngle);
                        if (difference < (double) closestDifference) {
                            closestDifference = (float) difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }
            }

            event.forward = closestForward;
            event.strafe = closestStrafe;
        }
    }

    /** direction из wonderful-1.21.4 MovingUtil */
    private static double directionFree(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0F) {
            rotationYaw += 180.0F;
        }
        float forward = 1.0F;
        if (moveForward < 0.0F) {
            forward = -0.5F;
        }
        if (moveForward > 0.0F) {
            forward = 0.5F;
        }
        if (moveStrafing > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }
        if (moveStrafing < 0.0F) {
            rotationYaw += 90.0F * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    private void resetRotation() {
        Rotation rotationTarget = new Rotation(FreeLookComponent.freeYaw, FreeLookComponent.freePitch);
        if (updateRotation(rotationTarget, returnYawSpeed, returnPitchSpeed)) {
            stopRotation();
        }
    }

    @Subscribe
    public void onEvent(EventTick event) {
        if (rotationState.equals(RotationState.AIM) && inactiveTicks > rotationTimeout) {
            rotationState = RotationState.RESET;
        }
        if (rotationState.equals(RotationState.RESET)) {
            resetRotation();
        }
        inactiveTicks++;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        if (rotationPriority > priority) return;
        if (rotationState.equals(RotationState.IDLE) && !clientRotation) {
            FreeLookComponent.active = true;
            if (mc.player != null) {
                movementYaw = mc.player.getYaw();
                FreeLookComponent.freeYaw = mc.player.getYaw();
                FreeLookComponent.freePitch = mc.player.getPitch();
            }
        }

        aimYawSpeed = yawSpeed;
        aimPitchSpeed = pitchSpeed;
        returnYawSpeed = yawReturnSpeed;
        returnPitchSpeed = pitchReturnSpeed;
        rotationTimeout = timeout;
        rotationPriority = priority;
        rotationState = RotationState.AIM;
        desiredRotation = target;

        updateRotation(target, yawSpeed, pitchSpeed);
    }

    public static void update(Rotation targetRotation, float turnSpeed,
                              float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private static boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null) return false;

        Rotation currentRotation = new Rotation(mc.player);
        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();

        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        float yaw = mc.player.getYaw();
        yaw += GCDUtil.getFixRotate(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw));
        mc.player.setYaw(yaw);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + GCDUtil.getFixRotate(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90F, 90F));

        inactiveTicks = 0;
        return new Rotation(mc.player).getDelta(targetRotation) < 1F;
    }

    public void stopRotation() {
        rotationState = RotationState.IDLE;
        rotationPriority = 0;
        FreeLookComponent.active = false;
    }

    public static boolean isRotating() {
        return !rotationState.equals(RotationState.IDLE);
    }

    public enum RotationState {
        AIM, RESET, IDLE
    }
}
