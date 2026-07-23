package dev.aethel.util.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.aethel.util.IMinecraft;

import java.util.Objects;

@UtilityClass
public class MovingUtil implements IMinecraft {

    public boolean hasPlayerMovement() {
        return mc.player.input.movementForward != 0f || mc.player.input.movementSideways != 0f;
    }

    public double[] calculateDirection(double distance) {
        return calculateDirection(mc.player.input.movementForward, mc.player.input.movementSideways, distance);
    }

    public double[] calculateDirection(float forward, float sideways, double distance) {
        float yaw = mc.player.getYaw();
        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += (forward > 0.0f) ? -45 : 45;
            } else if (sideways < 0.0f) {
                yaw += (forward > 0.0f) ? 45 : -45;
            }
            sideways = 0.0f;
            forward = (forward > 0.0f) ? 1.0f : -1.0f;
        }

        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
        double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;

        return new double[]{xMovement, zMovement};
    }

    public double getSpeedSqrt(Entity entity) {
        return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.prevX, entity.prevY, entity.prevZ)));
    }

    public void setVelocity(double velocity) {
        final double[] direction = MovingUtil.calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
    }

    public void setVelocity(double velocity, double y) {
        final double[] direction = MovingUtil.calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], y, direction[1]);
    }

    public double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
        float optimalYaw = (float) Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
        double currentYaw = Math.toRadians(MathHelper.wrapDegrees(yaw));
        return Math.toDegrees(MathHelper.wrapDegrees((optimalYaw - currentYaw)));
    }
}
