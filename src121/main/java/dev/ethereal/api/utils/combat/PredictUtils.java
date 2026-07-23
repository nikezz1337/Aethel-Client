package dev.ethereal.api.utils.combat;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PredictUtils {

    private static final Map<UUID, PositionData> entityData = new ConcurrentHashMap<>();

    public static Vec3d predict(LivingEntity entity, Vec3d pos, int ticks) {
        Vec3d velocity = entity.getVelocity();

        for (int i = 0; i < ticks; i++) {
            Vec3d rotation = entity.getRotationVector();
            float pitchRad = (float) Math.toRadians(entity.getPitch());
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double velocityLength = velocity.length();
            float cos = MathHelper.cos(pitchRad);
            cos = (float) (cos * cos * Math.min(1.0D, rotation.length() / 0.4D));

            velocity = velocity.add(0.0D, -0.08D * (-1.0D + (double) cos * 0.75D), 0.0D);

            if (velocity.y < 0.0D && horizontalSpeed > 0.0D) {
                double d5 = velocity.y * -0.1D * cos;
                velocity = velocity.add(rotation.x * d5 / horizontalSpeed, d5, rotation.z * d5 / horizontalSpeed);
            }

            if (pitchRad < 0.0F && horizontalSpeed > 0.0D) {
                double lift = velocityLength * (-MathHelper.sin(pitchRad)) * 0.04D;
                velocity = velocity.add(-rotation.x * lift / horizontalSpeed, lift * 3.2D, -rotation.z * lift / horizontalSpeed);
            }

            if (horizontalSpeed > 0.0D) {
                velocity = velocity.add(
                        (rotation.x / horizontalSpeed * velocityLength - velocity.x) * 0.1D,
                        0.0D,
                        (rotation.z / horizontalSpeed * velocityLength - velocity.z) * 0.1D
                );
            }

            velocity = velocity.multiply(0.99D, 0.98D, 0.99D);
            pos = pos.add(velocity);
        }

        return pos;
    }

    public static Vec3d predict(LivingEntity entity, int ticks) {
        Vec3d pos = entity.getPos();
        Vec3d velocity = entity.getVelocity();
        return pos.add(velocity.multiply(ticks));
    }

    public static void updateEntity(LivingEntity entity) {
        UUID uuid = entity.getUuid();
        PositionData data = entityData.get(uuid);
        Vec3d currentPos = entity.getPos();

        if (data == null) {
            entityData.put(uuid, new PositionData(currentPos, currentPos, System.currentTimeMillis()));
        } else {
            data.prevPos = data.currentPos;
            data.currentPos = currentPos;
            data.lastUpdateTime = System.currentTimeMillis();
        }
    }

    public static PositionData getData(LivingEntity entity) {
        return entityData.get(entity.getUuid());
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        entityData.entrySet().removeIf(entry -> now - entry.getValue().lastUpdateTime > 5000);
    }

    public static void clear() {
        entityData.clear();
    }

    public static class PositionData {
        Vec3d prevPos;
        Vec3d currentPos;
        long lastUpdateTime;

        public PositionData(Vec3d prevPos, Vec3d currentPos, long lastUpdateTime) {
            this.prevPos = prevPos;
            this.currentPos = currentPos;
            this.lastUpdateTime = lastUpdateTime;
        }

        public boolean isMoving() {
            return prevPos.squaredDistanceTo(currentPos) > 0.001;
        }
    }
}
