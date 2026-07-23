package dev.ethereal.api.utils.rotation;

import dev.ethereal.api.utils.math.MathUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.rotation.manager.Rotation;

@UtilityClass
public class RotationUtil implements QuickImports {
    public boolean inFov(Vec3d vec3d, float fov) {
        Rotation rotation = rotationAt(vec3d);
        float deltaYaw = MathHelper.wrapDegrees(rotation.getYaw() - mc.player.getYaw());
        float deltaPitch = MathHelper.wrapDegrees(rotation.getPitch() - mc.player.getPitch());

        return Math.abs(deltaYaw) <= fov && Math.abs(deltaPitch) <= fov;
    }

    public Rotation rotationAt(Vec3d position) {
        if (position == null) return Rotation.DEFAULT;
        Vec3d playerPos = mc.player.getPos().add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
        float diffX = (float) (position.x - playerPos.x);
        float diffY = (float) (position.y - playerPos.y);
        float diffZ = (float) (position.z - playerPos.z);

        float dist = (float) Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new Rotation(yaw, pitch);
    }

    public Vec2f getRotations(Vec3d vec3d) {
        return getRotations(vec3d.x, vec3d.y, vec3d.z);
    }

    public Vec2f getRotations(double x, double y, double z) {
        double deltaX = x - mc.player.getX();
        double deltaY = y - mc.player.getEyeY();
        double deltaZ = z - mc.player.getZ();
        double distance = MathHelper.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));

        float yaw = (float) (MathHelper.atan2(deltaZ, deltaX) * (180D / Math.PI) - 90.0F);
        float pitch = (float) (-MathHelper.atan2(deltaY, distance) * (180D / Math.PI));
        return new Vec2f(yaw, pitch);
    }

    public static float calculateCorrectYawOffset(float yaw) {
        if (mc.player == null) return yaw;

        double xDiff = mc.player.getX() - mc.player.prevX;
        double zDiff = mc.player.getZ() - mc.player.prevZ;
        float distSquared = (float) (xDiff * xDiff + zDiff * zDiff);
        float bodyYaw = mc.player.prevBodyYaw;
        float offset = bodyYaw;

        if (distSquared > 0.0025000002f) {
            offset = (float) MathHelper.atan2(zDiff, xDiff) * 180.0f / (float) Math.PI - 90.0f;
        }

        if (mc.player.handSwinging) {
            offset = yaw;
        }

        float yawOffsetDiff = MathHelper.wrapDegrees(yaw - (bodyYaw + MathHelper.wrapDegrees(offset - bodyYaw) * 0.3f));
        yawOffsetDiff = MathHelper.clamp(yawOffsetDiff, -75.0f, 75.0f);
        bodyYaw = yaw - yawOffsetDiff;
        if (yawOffsetDiff * yawOffsetDiff > 2500.0f) {
            bodyYaw += yawOffsetDiff * 0.2f;
        }

        return bodyYaw;
    }

    public Rotation fromVec2f(Vec2f vector2f) {
        return new Rotation(vector2f.y, vector2f.x);
    }

    public Rotation fromVec3d(Vec3d vector) {
        return new Rotation(
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90),
                MathHelper.wrapDegrees((float) Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
        );
    }

    public Rotation calculateDelta(Rotation start, Rotation end) {
        float deltaYaw = MathHelper.wrapDegrees(end.getYaw() - start.getYaw());
        float deltaPitch = MathHelper.wrapDegrees(end.getPitch() - start.getPitch());
        return new Rotation(deltaYaw, deltaPitch);
    }

    public Vec3d getSpot(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        return new Vec3d(
                MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ)
        );
    }

    public static Vec3d getVector3(LivingEntity target) {
        if (mc.player == null) return Vec3d.ZERO;
        return new Vec3d(
                target.getX() - mc.player.getX(),
                target.getY() + MathUtil.random(0, target.getHeight()) - mc.player.getEyeY(),
                target.getZ() - mc.player.getZ()
        );
    }

    public static float strictDistance(LivingEntity target) {
        return (float) mc.player.getEyePos().distanceTo(getSpot(target));
    }

    public Vec3d rayCastBox(Entity entity, Vec3d end) {
        Box box = entity.getBoundingBox();
        Vec3d start = mc.getCameraEntity() != null ? mc.getCameraEntity().getCameraPosVec(1.0f) : mc.player.getEyePos();

        Vec3d min = new Vec3d(box.minX, box.minY, box.minZ);
        Vec3d max = new Vec3d(box.maxX, box.maxY, box.maxZ);

        double tMin = -Double.MAX_VALUE;
        double tMax = Double.MAX_VALUE;
        Vec3d ray = end.subtract(start);
        double distance = ray.length();
        if (distance < 1e-7) {
            return end;
        }
        Vec3d direction = ray.normalize();

        for (int axis = 0; axis < 3; axis++) {
            double d, minVal, maxVal, startVal;

            switch (axis) {
                case 0: d = direction.x; minVal = min.x; maxVal = max.x; startVal = start.x; break;
                case 1: d = direction.y; minVal = min.y; maxVal = max.y; startVal = start.y; break;
                case 2: d = direction.z; minVal = min.z; maxVal = max.z; startVal = start.z; break;
                default: continue;
            }

            if (Math.abs(d) < 1e-7) {
                if (startVal < minVal || startVal > maxVal) {
                    return end;
                }
            } else {
                double t1 = (minVal - startVal) / d;
                double t2 = (maxVal - startVal) / d;

                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }

                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);

                if (tMin > tMax) {
                    return end;
                }
            }
        }

        if (tMin > distance || tMin < 0) {
            return end;
        }

        return start.add(direction.multiply(tMin));
    }

    public Vec2f calculate(final Vec3d fromVec, final Vec3d toVec) {
        final double TO_DEGREES = 180.0F / Math.PI;
        final Vec3d diff = toVec.subtract(fromVec);
        final double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.y, distance) * TO_DEGREES));
        return new Vec2f(yaw, pitch);
    }

    public static float getGCDValue() {
        return (float) (getGCD() * 0.15);
    }

    public static float getGCD() {
        float f1;
        return (f1 = (float) (mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2)) * f1 * f1 * 8;
    }
}
