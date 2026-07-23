package dev.ethereal.api.utils.combat;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PredictUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<UUID, PositionData> positionCache = new ConcurrentHashMap<>();

    @Getter
    public static class PositionData {
        private double serverX, serverY, serverZ;
        private double prevServerX, prevServerY, prevServerZ;
        private double lastSpeed, prevSpeed;
        private long lastUpdate;

        public Vec3d getResolvedPos() {
            return new Vec3d(serverX, serverY, serverZ);
        }

        public Vec3d getResolvedForward() {
            return new Vec3d(
                    serverX - prevServerX,
                    serverY - prevServerY,
                    serverZ - prevServerZ
            );
        }

        public void update(double x, double y, double z) {
            prevServerX = serverX;
            prevServerY = serverY;
            prevServerZ = serverZ;
            serverX = x;
            serverY = y;
            serverZ = z;
            prevSpeed = lastSpeed;
            lastSpeed = getResolvedForward().length() * 20;
            lastUpdate = System.currentTimeMillis();
        }

        public boolean isMoving() {
            return lastSpeed > 0.5;
        }
    }

    public static void updateEntity(LivingEntity entity) {
        PositionData data = positionCache.computeIfAbsent(entity.getUuid(), k -> new PositionData());
        data.update(entity.getX(), entity.getY(), entity.getZ());
    }

    public static PositionData getData(LivingEntity entity) {
        return positionCache.get(entity.getUuid());
    }

    /**
     * Предсказывает позицию цели через ticks тиков.
     * Для элитры использует физику, для обычного движения — линейный предикт.
     */
    public static Vec3d predict(LivingEntity entity, int ticks) {
        // Базовая позиция — центр тела чуть выше (чтобы целиться в торс)
        Vec3d pos = new Vec3d(entity.getX(), entity.getY() + entity.getStandingEyeHeight() * 0.75, entity.getZ());
        PositionData data = getData(entity);

        if (data == null || !data.isMoving()) return pos;

        if (entity.isGliding()) {
            return predictElytraPhysics(entity, pos, ticks);
        }

        // Линейный предикт по вектору движения
        Vec3d forward = data.getResolvedForward();
        return pos.add(forward.multiply(ticks, ticks, ticks));
    }

    public static Vec3d predictElytraPhysics(LivingEntity entity, Vec3d pos, int ticks) {
        Vec3d velocity = entity.getVelocity();

        double horizontalDelta = Math.hypot(entity.prevX - entity.getX(), entity.prevZ - entity.getZ()) * 20;
        double verticalDelta = Math.abs(entity.getY() - entity.prevY) * 20;

        if (horizontalDelta <= 5 && verticalDelta <= 5) return pos;

        for (int i = 0; i < ticks; i++) {
            Vec3d rotation = entity.getRotationVector();
            float pitchRad = (float) Math.toRadians(entity.getPitch());
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double velocityLength = velocity.length();
            float cos = MathHelper.cos(pitchRad);
            cos = (float) (cos * cos * Math.min(1.0, rotation.length() / 0.4));

            velocity = velocity.add(0, -0.08 * (-1.0 + cos * 0.75), 0);

            if (velocity.y < 0 && horizontalSpeed > 0) {
                double d = velocity.y * -0.1 * cos;
                velocity = velocity.add(rotation.x * d / horizontalSpeed, d, rotation.z * d / horizontalSpeed);
            }

            if (pitchRad < 0 && horizontalSpeed > 0) {
                double lift = velocityLength * (-MathHelper.sin(pitchRad)) * 0.04;
                velocity = velocity.add(-rotation.x * lift / horizontalSpeed, lift * 3.2, -rotation.z * lift / horizontalSpeed);
            }

            if (horizontalSpeed > 0) {
                velocity = velocity.add(
                        (rotation.x / horizontalSpeed * velocityLength - velocity.x) * 0.1,
                        0,
                        (rotation.z / horizontalSpeed * velocityLength - velocity.z) * 0.1
                );
            }

            velocity = velocity.multiply(0.99, 0.98, 0.99);
            pos = pos.add(velocity);
        }

        return pos;
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        positionCache.entrySet().removeIf(e -> now - e.getValue().getLastUpdate() > 10000);
    }

    public static void clear() {
        positionCache.clear();
    }
}
