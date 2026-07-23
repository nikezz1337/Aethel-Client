package dev.aethel.module.list.combat.aura;

import dev.aethel.module.list.combat.aura.rotation.FreeLookComponent;
import dev.aethel.module.list.combat.aura.rotation.Rotation;
import dev.aethel.module.list.combat.aura.rotation.URotations;
import dev.aethel.module.list.combat.aura.util.Mathf;
import dev.aethel.module.list.combat.aura.util.time.TimerUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.toDegrees;

public class URotate {
    private static float tick = 0;
    private static final TimerUtil timer = new TimerUtil();
    private static final TimerUtil timer2 = new TimerUtil();
    private static final net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

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

    private static float lastYaw;
    private static float lastPitch;
    private static boolean pitchFlickActive = false;
    private static long pitchFlickEndTime = 0;
    private static boolean justAttacked = false;
    private static long attackFlickAt = 0;

    // HolyWorld aim region state — меняется при каждом ударе
    private static float holyAimOffsetY = 0.5f;
    private static float holyAimOffsetX = 0f;
    private static float holyAimOffsetZ = 0f;
    private static boolean holyLastCanAttack = false;
    private static int holyHitCount = 0;
    // HolyWorld timers для динамических скоростей
    private static final TimerUtil holyTimerSpeed = new TimerUtil();
    private static final TimerUtil holyTimerSpeed2 = new TimerUtil();
    private static final TimerUtil holyTimerIdleSpeed = new TimerUtil();
    private static final TimerUtil holyTimerIdleSpeed2 = new TimerUtil();

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

    public static void onHolyRotation(LivingEntity target, float[] ranges) {
        if (mc.player == null || target == null) return;
        if (mc.player.isSubmergedInWater()) return;

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        boolean canAttack = UAttack.shouldAttack(target, false, true, true,
                (long) -Mathf.random(150, 250), ranges);

        if (canAttack && !holyLastCanAttack) {
            holyHitCount++;

            float r = rand.nextFloat();
            if (r < 0.30f) {
                holyAimOffsetY = 0.75f + rand.nextFloat() * 0.25f;
            } else if (r < 0.60f) {
                holyAimOffsetY = 0.45f + rand.nextFloat() * 0.30f;
            } else if (r < 0.82f) {
                holyAimOffsetY = 0.15f + rand.nextFloat() * 0.30f;
            } else {
                holyAimOffsetY = rand.nextFloat() * 0.20f;
            }

            holyAimOffsetX = (rand.nextFloat() - 0.5f) * 0.35f;
            holyAimOffsetZ = (rand.nextFloat() - 0.5f) * 0.35f;

            if (holyHitCount % 7 == 0) {
                holyAimOffsetX = (rand.nextFloat() - 0.5f) * 0.7f;
            }
        }
        holyLastCanAttack = canAttack;

        float eyeHeight = target.getStandingEyeHeight();
        float baseY = (float) (target.getY() + eyeHeight * 0.75f);
        float topY = (float) (target.getY() + eyeHeight * 1.0f);

        float targetY = baseY + (topY - baseY) * holyAimOffsetY;

        Vec3d aimPoint = new Vec3d(
                target.getX() + holyAimOffsetX,
                targetY,
                target.getZ() + holyAimOffsetZ
        );

        Vec3d toTarget = aimPoint.subtract(mc.player.getEyePos());

        float rawYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float rawPitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))),
                -90F, 90F);

        float randomBoga = (float) (Mathf.random(-1, 1) * Mathf.random(-1, 1)
                * Mathf.randomInt(-2, 2)
                * ThreadLocalRandom.current().nextDouble(-4.3535, 3.3553));

        double yawJitter = randomBoga * 3.3F;
        double pitchJitter = randomBoga * 3.1F;

        float speed, speed2;

        if (pitchFlickActive) {
            speed = Mathf.randomWithUpdate(62, 72, 40, holyTimerSpeed);
            speed2 = Mathf.randomWithUpdate(24, 40, 50, holyTimerSpeed2);
        } else if (canAttack) {
            speed = Mathf.randomWithUpdate(58, 72, 30 + (holyHitCount % 7) * 5, holyTimerSpeed);
            speed2 = Mathf.randomWithUpdate(50, 68, 40 + (holyHitCount % 5) * 10, holyTimerSpeed2);
        } else {
            speed = Mathf.randomWithUpdate(14, 28, 300 + (holyHitCount % 11) * 30, holyTimerIdleSpeed);
            speed2 = Mathf.randomWithUpdate(10, 22, 400 + (holyHitCount % 9) * 25, holyTimerIdleSpeed2);
        }


        if (canAttack && !holyLastCanAttack && holyHitCount > 0 && holyHitCount % 50 == 0) {
            pitchFlickActive = true;
            pitchFlickEndTime = System.currentTimeMillis() + 240 + rand.nextInt(80);
        }

        if (pitchFlickActive) {
            if (System.currentTimeMillis() > pitchFlickEndTime) {
                pitchFlickActive = false;
            } else {
                lastYaw = mc.player.getYaw() - (rand.nextBoolean()
                        ? -Mathf.random(40, 85) : Mathf.random(40, 85));
                lastPitch = (rand.nextBoolean()
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
                Mathf.random(40.12481248124F, 90.412841824F),
                Mathf.random(40.12481248124F, 90.412841824F),
                0, 1, false
        );
    }

    public static void onFunTimeRotation(LivingEntity target, float[] ranges) {
        UFuntime.rotation(target, ranges);
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
