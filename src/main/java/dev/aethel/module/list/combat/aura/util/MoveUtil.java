package dev.aethel.module.list.combat.aura.util;

import dev.aethel.event.list.MoveInputEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.MathHelper;

public class MoveUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void fixMovement(MoveInputEvent event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();

        if (forward == 0 && strafe == 0) return;

        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, forward, strafe)));

        float bestForward = 0, bestStrafe = 0;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward++) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe++) {
                if (testForward == 0 && testStrafe == 0) continue;
                double testAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, testForward, testStrafe)));
                float difference = Math.abs(MathHelper.wrapDegrees((float) (angle - testAngle)));
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


    public KeyBinding[] getMovementKeys() {
        return new KeyBinding[]{
                mc.options.sprintKey,
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey
        };
    }

    public static void targetMove(MoveInputEvent event, float currentYaw, float targetYaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();

        if (forward == 0 && strafe == 0) return;

        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(currentYaw, forward, strafe)));

        float bestForward = 0, bestStrafe = 0;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward++) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe++) {
                if (testForward == 0 && testStrafe == 0) continue;
                double testAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(targetYaw, testForward, testStrafe)));
                float difference = Math.abs(MathHelper.wrapDegrees((float) (angle - testAngle)));
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
        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        if (moveForward > 0F) forward = 0.5F;
        if (moveStrafing > 0F) yaw -= 90F * forward;
        if (moveStrafing < 0F) yaw += 90F * forward;
        return Math.toRadians(yaw);
    }
}
