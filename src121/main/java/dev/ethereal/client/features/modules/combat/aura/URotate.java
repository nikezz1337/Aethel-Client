package dev.ethereal.client.features.modules.combat.aura;

import dev.ethereal.client.features.modules.combat.aura.rotation.FreeLookComponent;
import dev.ethereal.client.features.modules.combat.aura.rotation.Rotation;
import dev.ethereal.client.features.modules.combat.aura.rotation.URotations;
import dev.ethereal.client.features.modules.combat.aura.util.Mathf;
import dev.ethereal.client.features.modules.combat.aura.util.time.TimerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.toDegrees;

public class URotate {
    private static float tick = 0;
    private static final TimerUtil timer = new TimerUtil();
    private static final TimerUtil timer2 = new TimerUtil();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static float aimOffsetY = 0.5f;
    private static float aimOffsetX = 0f;
    private static float aimOffsetZ = 0f;
    private static float storedJitterYaw = 0f;
    private static float storedJitterPitch = 0f;
    private static float attackTicks = 0;
    private static float holdTicks = 0;

    private static float savedYaw = 0f;
    private static float savedPitch = 0f;
    private static boolean hasSavedRotation = false;

    public static void onSpookyRotation(LivingEntity target, boolean attack, boolean inAttackRange) {
        if (target == null || mc.player == null) return;

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        if (attack) {
            float r = rand.nextFloat();
            if (r < 0.50f) {
                aimOffsetY = 0.70f + rand.nextFloat() * 0.30f;
            } else if (r < 0.78f) {
                aimOffsetY = 0.40f + rand.nextFloat() * 0.30f;
            } else {
                aimOffsetY = 0.05f + rand.nextFloat() * 0.35f;
            }

            aimOffsetX = (rand.nextFloat() - 0.5f) * 0.2f;
            aimOffsetZ = (rand.nextFloat() - 0.5f) * 0.2f;

            storedJitterYaw = (rand.nextFloat() - 0.5f) * (2.0f + rand.nextFloat() * 1.0f);
            storedJitterPitch = (rand.nextFloat() - 0.5f) * (1.5f + rand.nextFloat() * 0.8f);

            attackTicks = 1;
            holdTicks = 8;
        }

        boolean postAttack = attackTicks > 0;
        if (postAttack) attackTicks--;

        boolean holding = holdTicks > 0;
        if (holding) {
            holdTicks--;
            if (holdTicks <= 0) {
                hasSavedRotation = false;
            }
        }

        float eyeHeight = target.getStandingEyeHeight();
        float chinY = (float) (target.getY() + eyeHeight * 0.75f);
        float topY = (float) (target.getY() + eyeHeight * 1.0f);
        float targetY = chinY + (topY - chinY) * aimOffsetY;

        Vec3d aimPoint = new Vec3d(target.getX() + aimOffsetX, targetY, target.getZ() + aimOffsetZ);
        Vec3d toTarget = aimPoint.subtract(mc.player.getEyePos());

        float desiredYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float desiredPitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))),
                -89f, 90f);

        float curYaw = mc.player.getYaw();
        float curPitch = mc.player.getPitch();

        float maxStep = 26f + rand.nextFloat() * 8f;
        float smooth = 0.88f + rand.nextFloat() * 0.12f;

        long t = System.currentTimeMillis();
        float p1 = 70f + rand.nextFloat() * 30f;
        float p2 = 40f + rand.nextFloat() * 20f;
        float p3 = 110f + rand.nextFloat() * 30f;
        float p4 = 50f + rand.nextFloat() * 20f;
        float swayYaw = (float) (Math.sin(t / p1) * (1.0 + rand.nextFloat() * 0.8) + Math.cos(t / p2) * (2.0 + rand.nextFloat() * 2.0));
        float swayPitch = (float) (Math.sin(t / p3) * (1.0 + rand.nextFloat() * 0.6) + Math.cos(t / p4) * (1.0 + rand.nextFloat() * 1.0));

        float nextYaw, nextPitch;

        if (!inAttackRange) {
            float deltaYaw = MathHelper.wrapDegrees(desiredYaw - curYaw);
            float deltaPitch = desiredPitch - curPitch;
            nextYaw = curYaw + clampStep(deltaYaw * smooth, maxStep) + swayYaw;
            nextPitch = MathHelper.clamp(curPitch + clampStep(deltaPitch * smooth, maxStep) + swayPitch, -89f, 90f);

        } else if (attack || postAttack) {
            float deltaYaw = MathHelper.wrapDegrees(desiredYaw - curYaw);
            float deltaPitch = desiredPitch - curPitch;
            nextYaw = curYaw + clampStep(deltaYaw * smooth, maxStep) + storedJitterYaw + swayYaw;
            nextPitch = MathHelper.clamp(curPitch + clampStep(deltaPitch * smooth, maxStep) + storedJitterPitch + swayPitch, -89f, 90f);

            savedYaw = nextYaw;
            savedPitch = nextPitch;
            hasSavedRotation = true;

        } else if (hasSavedRotation && holding) {
            nextYaw = savedYaw + swayYaw;
            nextPitch = MathHelper.clamp(savedPitch + swayPitch, -89f, 90f);

        } else {
            float deltaYaw = MathHelper.wrapDegrees(desiredYaw - curYaw);
            float deltaPitch = desiredPitch - curPitch;
            nextYaw = curYaw + clampStep(deltaYaw * smooth, maxStep) + swayYaw;
            nextPitch = MathHelper.clamp(curPitch + clampStep(deltaPitch * smooth, maxStep) + swayPitch, -89f, 90f);
        }

        URotations.update(
                new Rotation(nextYaw, nextPitch),
                Mathf.randomWithUpdate(50, 67, 35, timer),
                Mathf.randomWithUpdate(10, 15, 100, timer2),
                Mathf.randomWithUpdate(15, 27, 35, timer),
                Mathf.randomWithUpdate(20, 67, 100, timer2),
                2, 15, false
        );
    }

    private static float clampStep(float delta, float maxStep) {
        return MathHelper.clamp(delta, -maxStep, maxStep);
    }

    public static void onHolyRotation(LivingEntity target, boolean attack) {
        long t = System.currentTimeMillis();
        float addyVacY = 0.2F * (float) Math.cos(t / 1500D);
        float addyVacZ = 0.15F * (float) Math.cos(t / 700D);
        float addyVacX = 0.16F * (float) Math.cos(t / 900D);

        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d vec = target.getPos()
                .add(addyVacX, MathHelper.clamp(playerEyePos.y - target.getY(), 0.0F, 0.8) - addyVacY, addyVacZ)
                .subtract(playerEyePos).normalize();

        boolean attackF = false;
        if (attack) tick = 2;
        if (tick > 0) {
            attackF = true;
            tick--;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90, 90);
        float randomToAttack = attackF ? (float) (Math.sin(t / 30D) * 2) * (float) (Math.cos(t / 40D) * 3) : 0;

        Rotation newRotation = new Rotation(
                yaw + randomToAttack + (float) (Math.sin(t / 80D) * 2) + (float) (Math.cos(t / 50D) * 4),
                pitch + randomToAttack + (float) (Math.sin(t / 120D) * 3) + (float) (Math.cos(t / 60D) * 2));
        URotations.update(newRotation, Mathf.randomWithUpdate(30, 40, 35, timer),
                Mathf.randomWithUpdate(20, 30, 100, timer2), 32, 32, 0, 15, false);
    }

    public static void onFunTimeRotation(LivingEntity target, boolean attack, float attackD, boolean check) {
        UFuntime.rotation(target, attack, attackD, check);
    }

    public static void onSnapRotation(LivingEntity target, boolean attack) {
        float addyVacY = 0.4F * (float) Math.cos(System.currentTimeMillis() / 1500D);
        float addyVacZ = 0.25F * (float) Math.cos(System.currentTimeMillis() / 700D);
        float addyVacX = 0.36F * (float) Math.cos(System.currentTimeMillis() / 900D);

        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d vec = target.getPos()
                .add(addyVacX, MathHelper.clamp(playerEyePos.y - target.getY(), 0.0F, 0.8) - addyVacY, addyVacZ)
                .subtract(playerEyePos).normalize();

        float yaw = FreeLookComponent.getFreeYaw();
        float pitch = FreeLookComponent.getFreePitch();
        float speed = Mathf.randomInt(27, 33);

        boolean attackF = false;
        if (attack) tick = 5;
        if (tick > 0) {
            attackF = true;
            tick--;
        }

        if (attackF) {
            speed = Mathf.randomInt(50, 70);
            yaw = (float) toDegrees(Math.atan2(-vec.x, vec.z)) + ThreadLocalRandom.current().nextFloat(-3, 3);
            pitch = (float) MathHelper.clamp(-toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F)
                    + ThreadLocalRandom.current().nextFloat(-3, 3);
        }

        URotations.update(new Rotation(
                yaw + ThreadLocalRandom.current().nextFloat(-1, 1), pitch),
                speed, 67, 1, 7);
    }

    public static void onMatrixRotation(LivingEntity target, boolean attack) {
        float addyVacY = 0.4F * (float) Math.cos(System.currentTimeMillis() / 1500D);
        float addyVacZ = 0.25F * (float) Math.cos(System.currentTimeMillis() / 1200D);
        float addyVacX = 0.28F * (float) Math.cos(System.currentTimeMillis() / 1400D);

        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d vec2 = AuraUtil.getSpookyVector(target).add(addyVacX, 0, addyVacZ);
        Vec3d vec = target.getPos()
                .add(addyVacX, MathHelper.clamp(playerEyePos.y - target.getY(), 0.0F, 0.8) - addyVacY, addyVacZ)
                .subtract(playerEyePos).normalize();

        boolean attackF = false;
        if (attack) tick = 2;
        if (tick > 0) {
            attackF = true;
            tick--;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-vec2.x, vec2.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec2.y, Math.hypot(vec2.x, vec2.z))), -90, 90);

        Rotation newRotation = new Rotation(yaw + Mathf.randomNew(-3, 3), pitch + ThreadLocalRandom.current().nextFloat(-1, 1));
        URotations.update(newRotation, Mathf.randomInt(35, 40),
                Mathf.randomInt(7, 14), 16, 16, 10, 15, false);
    }
}
