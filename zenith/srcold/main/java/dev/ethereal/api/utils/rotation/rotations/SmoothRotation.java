package dev.ethereal.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationMode;

import java.util.Random;

public class SmoothRotation extends RotationMode {

    private static final Random RANDOM = new Random();

    private float interpYaw = Float.NaN;

    private float wanderYaw = 0f;
    private float wanderTarget = 0f;
    private long lastWanderUpdate = 0;

    private float prevPlayerYaw = Float.NaN;
    private float playerYawVelocity = 0f;

    public SmoothRotation() {
        super("Smooth");
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3d vec3d, Entity entity) {
        float playerPitch = mc.player != null ? mc.player.getPitch() : current.getPitch();

        if (Float.isNaN(interpYaw)) interpYaw = current.getYaw();

        float realPlayerYaw = mc.player != null ? mc.player.getYaw() : current.getYaw();
        if (Float.isNaN(prevPlayerYaw)) prevPlayerYaw = realPlayerYaw;
        playerYawVelocity = MathHelper.wrapDegrees(realPlayerYaw - prevPlayerYaw);
        prevPlayerYaw = realPlayerYaw;

        float targetYaw = target.getYaw();
        float yawDelta  = MathHelper.wrapDegrees(targetYaw - interpYaw);
        float absYaw    = Math.abs(yawDelta);

        float distFactor = MathUtil.clamp(absYaw / 90f, 0.08f, 1f);
        float speed = MathUtil.random(35f, 43f) * distFactor;

        float step = MathHelper.clamp(yawDelta, -speed, speed);
        interpYaw = MathHelper.wrapDegrees(interpYaw + step);

        long now = System.currentTimeMillis();
        if (now - lastWanderUpdate > RANDOM.nextInt(120) + 80) {
            lastWanderUpdate = now;
            float jitterAmp = MathUtil.clamp(absYaw / 30f, 0.05f, 2.5f);
            float playerInfluence = Math.abs(playerYawVelocity);
            if (playerInfluence > 1.5f) {
                jitterAmp += MathUtil.clamp(playerInfluence * 0.3f, 0f, 2f);
                wanderTarget = (float)(
                        RANDOM.nextGaussian() * 0.5
                                + Math.random() * Math.random() * 1.5
                                + Math.sin(now / 90.0) * 0.7
                ) * jitterAmp + playerYawVelocity * 0.25f;
            } else {
                wanderTarget = (float)(
                        RANDOM.nextGaussian() * 0.5
                                + Math.random() * Math.random() * 1.5
                                + Math.sin(now / 90.0) * 0.7
                ) * jitterAmp;
            }
        }
        wanderYaw = MathHelper.lerp(0.25f, wanderYaw, wanderTarget);

        float finalYaw = interpYaw + wanderYaw;

        return new Rotation(finalYaw, playerPitch);
    }
}
