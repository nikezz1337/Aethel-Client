package dev.aethel.module.list.combat.aura;

import dev.aethel.module.list.combat.aura.rotation.Rotation;
import dev.aethel.module.list.combat.aura.rotation.URotations;
import dev.aethel.module.list.combat.aura.util.Mathf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public class UFuntime {
    private static final net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

    // Состояние ротации (адаптировано из FunTimeRotation)
    private static float lastYaw;
    private static float lastPitch;
    private static boolean pitchFlickActive = false;
    private static long pitchFlickEndTime = 0;
    private static boolean justAttacked = false;
    private static long attackFlickAt = 0;

    public static void rotation(LivingEntity target, float[] ranges) {
        if (mc.player == null || target == null) return;
        if (mc.player.isSubmergedInWater()) return;


        Vec3d vec = target.getPos()
                .add(0, target.getHeight() / 2 + (target.getHeight() / 4) * Math.sin(System.currentTimeMillis() / 500L), 0)
                .subtract(mc.player.getEyePos())
                .normalize();

        float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))),
                -90F, 90F);

        float randomBoga = (float) (Mathf.random(-1, 1) * Mathf.random(-1, 1) * Mathf.randomInt(-2, 2) * ThreadLocalRandom.current().nextDouble(-4.3535, 3.3553));

        double yawJitter = randomBoga * 3.3F;
        double pitchJitter = randomBoga * 3.1F;

        boolean canAttack = UAttack.shouldAttack(target, false, true, true,
                (long) -Mathf.random(150, 250), ranges);

        float speed = Mathf.random(75, 80);
        float speed2 = pitchFlickActive ? Mathf.random(24, 40) : Mathf.random(45, 65);

        if (pitchFlickActive) {
            if (System.currentTimeMillis() > pitchFlickEndTime) {
                pitchFlickActive = false;
            } else {
                lastYaw = mc.player.getYaw() - (ThreadLocalRandom.current().nextBoolean()
                        ? -Mathf.random(40, 85) : Mathf.random(40, 85));
                lastPitch = (ThreadLocalRandom.current().nextBoolean()
                        ? -Mathf.random(70, 90) : Mathf.random(70, 90));
            }
        }

        if (canAttack) {
            if (!pitchFlickActive) lastPitch = rawPitch;
            if (!pitchFlickActive) lastYaw = rawYaw;
        }

        if (justAttacked && System.currentTimeMillis() >= attackFlickAt) {
            justAttacked = false;
        }

        URotations.update(
                new Rotation((float) (lastYaw + yawJitter), (float) (lastPitch + pitchJitter)),
                speed,
                speed2,
                Mathf.random(40.12481248124F, 140.412841824F),
                Mathf.random(40.12481248124F, 140.412841824F),
                0, 1, false
        );
    }
}
