package dev.aethel.util.combat;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtils {

    public static float[] getRotations(Vec3d target) {
        return getRotations(target, null);
    }

    public static float[] getRotations(Vec3d target, Vec3d eyePos) {
        if (eyePos == null) {
            eyePos = net.minecraft.client.MinecraftClient.getInstance().player.getEyePos();
        }
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }

    public static float smoothRotation(float current, float target, float speed) {
        float diff = MathHelper.wrapDegrees(target - current);
        return current + diff * speed;
    }
}
