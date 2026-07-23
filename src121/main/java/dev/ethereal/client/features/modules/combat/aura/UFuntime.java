package dev.ethereal.client.features.modules.combat.aura;

import dev.ethereal.client.features.modules.combat.Aura;
import dev.ethereal.client.features.modules.combat.aura.rotation.FreeLookComponent;
import dev.ethereal.client.features.modules.combat.aura.rotation.Rotation;
import dev.ethereal.client.features.modules.combat.aura.rotation.URotations;
import dev.ethereal.client.features.modules.combat.aura.util.Mathf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public class UFuntime {
    private static float tick;
    private static float lastAttackPitch = 0;

    public static void rotation(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        long currentTime = System.currentTimeMillis();

        if (!Aura.getInstance().isLookingUp &&
                currentTime - Aura.getInstance().lastLookUpTime >= Aura.getInstance().nextLookUpDelay) {

            Aura.getInstance().isLookingUp = true;
            Aura.getInstance().lookUpStartTime = currentTime;
            Aura.getInstance().lookUpDuration = ThreadLocalRandom.current().nextInt(270, 390);
            Aura.getInstance().lastLookUpTime = currentTime;
            Aura.getInstance().nextLookUpDelay = ThreadLocalRandom.current().nextLong(6500, 7200);
        }

        boolean fastspeed = false;
        if (Aura.getInstance().isLookingUp && currentTime - Aura.getInstance().lookUpStartTime >= Aura.getInstance().lookUpDuration) {
            Aura.getInstance().isLookingUp = false;
        }
        if (currentTime - Aura.getInstance().lookUpStartTime >= Aura.getInstance().lookUpDuration + 40L) {
            fastspeed = true;
        }

        Vec3d playerEyePos = mc.player.getEyePos();

        float oscillY = (float) Math.cos(System.currentTimeMillis() / 450D);
        float offsetY = 0.06F * oscillY;

        float oscillZ = (float) Math.cos(System.currentTimeMillis() / 500D);
        float offsetZ = 0.06F * oscillZ;

        float oscillX = (float) Math.cos(System.currentTimeMillis() / 14000D);
        float offsetX = 0.5F * oscillX;

        Vec3d directionVec = UBoxPoints.getBestVector3dOnEntityBox(target).subtract(playerEyePos);

        float baseYaw = FreeLookComponent.getFreeYaw();

        if (isAttack && AuraUtil.getStrictDistance(target) < attackDistance && !check) {
            tick = Mathf.randomValue(6, 7);
        }

        float fov = AuraUtil.calculateFOVFromCamera(target);
        float baseFov = 360;
        float yawChangeSpeed = Mathf.randomValue(22, 29);
        float randomAttackShift = 0;
        float pitchChangeSpeed = Mathf.randomValue(0, 3.5F);
        float waveA = (float) Math.cos(System.currentTimeMillis() / 40D);
        float waveB = (float) Math.sin(System.currentTimeMillis() / 70D);

        if (tick > 0 && Math.abs(fov) < baseFov) {
            yawChangeSpeed = Mathf.randomValue(50, 90);
            baseYaw = (float) Math.toDegrees(Math.atan2(-directionVec.x, directionVec.z));
            randomAttackShift = (waveA + waveB) * Aura.getInstance().randomLerp(1, 2);
            tick--;
        }

        float basePitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(directionVec.y, Math.hypot(directionVec.x, directionVec.z))),
                -90, 90);

        float yawJitter = waveA * Aura.getInstance().randomLerp(13, 15) + randomAttackShift;
        float pitchJitter = waveB * Aura.getInstance().randomLerp(5, 7) + randomAttackShift;

        float calculatedPitch = basePitch;

        if (isAttack && AuraUtil.getStrictDistance(target) < attackDistance && !check) {
            lastAttackPitch = calculatedPitch;
        }

        float finalPitch = Aura.getInstance().isLookingUp ? -Mathf.randomValue(80, 90) : calculatedPitch;

        Rotation newRotation = new Rotation(baseYaw + yawJitter, finalPitch + pitchJitter);

        URotations.update(
                newRotation,
                yawChangeSpeed,
                (Aura.getInstance().isLookingUp) ? Aura.getInstance().randomLerp(120, 170) :
                        fastspeed ? Aura.getInstance().randomLerp(120, 170) : Aura.getInstance().randomLerp(6, 8),
                25, 25, 0, 15,
                false
        );
    }

    private static final MinecraftClient mc = MinecraftClient.getInstance();
}
