package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.combat.ClickScheduler;
import dev.ethereal.api.utils.combat.CombatExecutor;
import dev.ethereal.api.utils.combat.CombatManager;
import dev.ethereal.api.utils.math.Interpolator;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.misc.BestPoint;
import dev.ethereal.api.utils.rotation.misc.UBoxPoints;
import dev.ethereal.client.features.modules.combat.AuraModule;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.util.math.MathHelper.ceil;

public class CombatRotation {
    private static final MinecraftClient mc = MinecraftClient.getInstance();


    private static final TimerUtil timer = new TimerUtil();
    private static final TimerUtil timer2 = new TimerUtil();
    private static final TimerUtil timer3 = new TimerUtil();
    private static final TimerUtil timer4 = new TimerUtil();
    private static final TimerUtil timer5 = new TimerUtil();
    private static final TimerUtil timer6 = new TimerUtil();
    private static float getGCDValue() {
        float sens = (float) (MinecraftClient.getInstance()
                .options.getMouseSensitivity().getValue() * 0.6 + 0.2);
        float t = sens * sens * sens * 8.0f;
        return t * 0.15f;
    }
    private static int stAwayTicks = 0;
    private static int stAwayMax = 0;
    private static int stAwayCooldown = 100;
    private static float stAwayYaw = 0;
    private static float stAwayPitch = 0;

    private static float lastYaw = 0f;
    private static float lastPitch = 0f;
    public static int nextTick;

    private static float randomPitch = 0;

    // === rotST2 state ===
    /** Смещение точки прицела относительно позиции цели (блокируется в момент удара) */
    private static Vec3d st2LockOffset = null;

    public static void rotHW(LivingEntity target, boolean attack, boolean rayCast, boolean slowAttack) {
        if (target == null || mc.player == null) return;

        Vec3d point = BestPoint.getMultipoint(target, 5);
        Vec2f angle = RotationUtil.getRotations(point);
        float targetYaw = angle.x;
        float targetPitch = angle.y;

        float yawSpeed, pitchSpeed, attackSpeed;
        attackSpeed = attack ? 0.6f : 1.0f;
        yawSpeed = MathUtil.random(42f, 51f) * attackSpeed;
        pitchSpeed = MathUtil.random(7f, 10.5f) * attackSpeed;

        float randomUp = (float) ((MathUtil.random(18.0, 28.0) + MathUtil.random(22.0, 32.0)) / 2);
        float randomDown = (float) -((MathUtil.random(30.0, 40.0) + MathUtil.random(20.0, 35.0)) / 2);
        randomUp *= attack ? 0.3F : 1.0f;
        randomDown *= attack ? 0.3F : 1.0f;

        float randomYaw = (float) (Interpolator.randomLerp(2f, 4f) * Math.sin(System.currentTimeMillis() / 90D));

        if (nextTick == 0 && attack) {
            randomPitch = MathUtil.random(0, 1) == 0 ? randomUp : randomDown;
        }

        Rotation rotation = new Rotation(targetYaw, targetPitch);

        Rotation save = new Rotation(lastYaw + randomYaw, lastPitch + randomPitch);

        if (nextTick == 0 && attack) {
            lastYaw = rotation.getYaw();
            lastPitch = rotation.getPitch();
            nextTick = 9;
        }

        randomPitch = nextTick > 0 && randomPitch == 0 ? (MathUtil.random(0, 1) == 0 ? randomUp : randomDown) : randomPitch;

        RotationComponent.update(nextTick > 0 ? save : rotation,
                yawSpeed, pitchSpeed,
                MathUtil.random(14, 31), MathUtil.random(9, 19),
                MathUtil.random(0, 2), 15, false);

        if (nextTick > 0) {
            nextTick--;
        }

        if (attack && nextTick == 0) {
            lastYaw = targetYaw;
            lastPitch = targetPitch;
            randomPitch = 0;
        }
    }

    public static void rotST(LivingEntity target, boolean attack) {

        float gcd = getGCDValue();
        int tick = mc.player.age;

        Vec3d pos = target.getPos();
        double hw = target.getWidth() * 0.5;
        double bx = pos.x + ThreadLocalRandom.current().nextDouble(-hw, hw);
        double by = pos.y + ThreadLocalRandom.current().nextDouble(0.1, 0.35);
        double bz = pos.z + ThreadLocalRandom.current().nextDouble(-hw, hw);
        Vec3d feetPoint = new Vec3d(bx, by, bz);

        Vec2f angle = RotationUtil.getRotations(feetPoint);
        float targetYaw = angle.x - (angle.x % gcd);
        float targetPitch = MathHelper.clamp(angle.y - (angle.y % gcd), -85f, 85f);

        float awayOffsetYaw = 0f;
        float awayOffsetPitch = 0f;

        if (stAwayTicks > 0) {
            float progress = 1f - (float) stAwayTicks / stAwayMax;
            if (progress < 0.5f) {
                awayOffsetYaw = stAwayYaw * (progress * 2f);
                awayOffsetPitch = stAwayPitch * (progress * 2f);
            } else {
                awayOffsetYaw = stAwayYaw * (2f - progress * 2f);
                awayOffsetPitch = stAwayPitch * (2f - progress * 2f);
            }
            stAwayTicks--;
            if (stAwayTicks <= 0) {
                stAwayCooldown = ThreadLocalRandom.current().nextInt(80, 180);
            }
        } else {
            stAwayCooldown--;
            if (stAwayCooldown <= 0) {
                stAwayMax = ThreadLocalRandom.current().nextInt(3, 6);
                stAwayTicks = stAwayMax;
                stAwayYaw = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 50f;
                stAwayPitch = ThreadLocalRandom.current().nextFloat() * 25f + 5f;
            }
        }

        float tickF = tick * 0.05f;
        float jitterYaw = (float) (Math.sin(tickF * 12.0) * 2.5 + (Math.random() - 0.5) * 2.0);
        float jitterPitch = (float) (Math.sin(tickF * 9.5 + 1.1) * 2.0 + (Math.random() - 0.5) * 1.5);

        float finalYaw = targetYaw + jitterYaw + awayOffsetYaw;
        float finalPitch = MathHelper.clamp(targetPitch + jitterPitch + awayOffsetPitch, -85f, 85f);

        RotationComponent.update(new Rotation(finalYaw, finalPitch),
                MathUtil.random(50f, 65f), MathUtil.random(50f, 65f),
                MathUtil.random(50f, 65f), MathUtil.random(50f, 65f),
                2, 15, false);
    }

    public static void rotFT(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        if (target == null || mc.player == null) return;

        AuraModule aura = AuraModule.getInstance();
        long currentTime = System.currentTimeMillis();

        if (!aura.isLookingUp && currentTime - aura.lastLookUpTime >= aura.nextLookUpDelay) {
            aura.isLookingUp = true;
            aura.lookUpStartTime = currentTime;
            aura.lookUpDuration = ThreadLocalRandom.current().nextInt(300, 400);
            aura.lastLookUpTime = currentTime;
            aura.nextLookUpDelay = ThreadLocalRandom.current().nextLong(90_000L, 180_000L);
        }


        boolean fastSpeed = false;
        if (aura.isLookingUp && currentTime - aura.lookUpStartTime >= aura.lookUpDuration) {
            aura.isLookingUp = false;
        }
        if (currentTime - aura.lookUpStartTime >= aura.lookUpDuration + 70L) {
            fastSpeed = true;
        }
        double dist = mc.player.getEyePos().distanceTo(
                target.getPos().add(0, target.getHeight() * 0.5, 0)
        );
        double t = MathHelper.clamp((dist - 0.5) / 5.5, 0.0, 1.0);
        double heightFactor = 0.40 + t * 0.50;
        Vec3d aimPoint = st2LockOffset != null
                ? target.getPos().add(st2LockOffset)
                : target.getPos().add(0, target.getHeight() * heightFactor, 0);


        Vec3d vec = aimPoint.subtract(mc.player.getEyePos());
        float targetYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float targetPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);

        float speedYaw = MathUtil.random(75f, 95f);
        float speedPitch = !fastSpeed ? MathUtil.random(140f, 190f) : MathUtil.random(12f, 18f);
        ClickScheduler cs = new ClickScheduler();

        if (isAttack && RotationUtil.strictDistance(target) < attackDistance) {
            lastYaw = targetYaw;
            lastPitch = targetPitch;
        }

        if (mc.player.isSubmergedInWater()) {
            lastPitch = targetPitch;
        }

        Rotation current = RotationComponent.getInstance().currentRotation();
        if (current == null) {
            current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        float waveA = (float) Math.cos(System.currentTimeMillis() / 40.0D);
        float waveB = (float) Math.sin(System.currentTimeMillis() / 70.0D);

        float yawJitter = waveA * MathUtil.random(9f, 14.5f);
        float pitchJitter = waveB * MathUtil.random(2f, 3.5f);

        float finalPitch = aura.isLookingUp ? -MathUtil.random(-40, 90f) : lastPitch;

        float targetWithJitterYaw = lastYaw + yawJitter;
        float targetWithJitterPitch = finalPitch + pitchJitter;

        float yawDelta = MathHelper.wrapDegrees(targetWithJitterYaw - current.getYaw());
        float pitchDelta = targetWithJitterPitch - current.getPitch();

        float nextYaw = current.getYaw() + MathHelper.clamp(yawDelta, -speedYaw, speedYaw);
        float nextPitch = current.getPitch() + MathHelper.clamp(pitchDelta, -speedPitch, speedPitch);

        RotationComponent.update(
                new Rotation(nextYaw, MathHelper.clamp(nextPitch, -90f, 90f)),
                MathUtil.random(42,56), MathUtil.random(5,12),
                MathUtil.random(32,49), MathUtil.random(20,32), 0, 15,false, Easing.BACK_OUT);
    }

    public static void rotSnap(LivingEntity target, boolean attack, boolean rayTrace) {
        if (target == null || mc.player == null) return;

        Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(target, true)
                .subtract(mc.player.getEyePos())
                .normalize();

        float yaw = mc.getEntityRenderDispatcher().camera.getYaw();
        float pitch = mc.getEntityRenderDispatcher().camera.getPitch();

        float yawSpeed = getSens((float) MathUtil.randomTick(MathUtil.random(96, 106), MathUtil.random(141, 145), (long) Interpolator.randomLerp(40, 60), timer));
        float pitchSpeed = getSens((float) MathUtil.randomTick(MathUtil.random(54, 62), MathUtil.random(82, 87), (long) Interpolator.randomLerp(80, 90), timer2));

        float yawJitter = (float) (MathUtil.random(5, 6) * Math.cos(System.currentTimeMillis() / Interpolator.randomLerp(85, 92))
                + MathUtil.random(9, 11) * Math.sin(System.currentTimeMillis() / Interpolator.randomLerp(102, 109)));
        float pitchJitter = (float) (MathUtil.random(13, 14.4) * Math.cos(System.currentTimeMillis() / Interpolator.randomLerp(25, 33))
                + MathUtil.random(11, 14.9) * Math.sin(System.currentTimeMillis() / Interpolator.randomLerp(55, 64)));

        if (attack) {
            yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
            pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90, 90);
        }

        if (!attack) {
            yawSpeed = getSens((float) MathUtil.randomTick(MathUtil.random(45, 52), MathUtil.random(72, 76), (long) Interpolator.randomLerp(30, 44), timer3));
            pitchSpeed = getSens((float) MathUtil.randomTick(MathUtil.random(44, 49), MathUtil.random(49, 54), (long) Interpolator.randomLerp(60, 70), timer4));
            yawJitter = (float) (MathUtil.random(9, 10.2) * Math.cos(System.currentTimeMillis() / Interpolator.randomLerp(110, 130))
                    + MathUtil.random(12, 13.2) * Math.sin(System.currentTimeMillis() / Interpolator.randomLerp(80, 90)));
            pitchJitter = (float) (MathUtil.random(18.4, 19.2) * Math.cos(System.currentTimeMillis() / Interpolator.randomLerp(40, 50))
                    + MathUtil.random(18.2, 18.6) * Math.sin(System.currentTimeMillis() / Interpolator.randomLerp(30, 40)));
        }

        if (rayTrace) {
            yawSpeed = getSens(MathUtil.random(6, 11));
            pitchSpeed = getSens(MathUtil.random(4, 8f));
            yawJitter = MathUtil.random(-3, 4);
            pitchJitter = MathUtil.random(-9, 3);
        }

        RotationComponent.update(new Rotation(yaw + yawJitter, pitch + pitchJitter),
                yawSpeed, pitchSpeed,
                MathUtil.random(120, 150), MathUtil.random(80, 110),
                MathUtil.random(0, 1), 15, false);
    }

    public static void rotLony(LivingEntity target, boolean attack, boolean rayCast) {
        if (target == null || mc.player == null) return;

        float random = !rayCast
                ? (float) (MathUtil.random(0.6, 0.65) * Math.sin(System.currentTimeMillis() / 54D))
                : (float) (MathUtil.random(0.3, 0.4) * Math.sin(System.currentTimeMillis() / 65D));
        if (Math.random() - 0.88 > 0.1) {
            random *= MathUtil.random(0.8f, 1.4f);
        }

        Vec3d vec = RotationUtil.getVector3(target).add(random, random, random);
        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90, 90);

        float yawSpeed = attack ? MathUtil.random(60, 65) : MathUtil.random(45, 50);
        float pitchSpeed = attack ? MathUtil.random(4, 5.4f) : MathUtil.random(2, 3);

        if (mc.player.age % ceil(MathUtil.random(
                getSens(MathUtil.random(120, 130)) / 30 * 1.25f,
                getSens(MathUtil.random(140, 150)) / 30 * 5f)) == 0) {
            yawSpeed = MathUtil.random(3, 6);
            pitchSpeed = MathUtil.random(0.5f, 1.05f);
        }

        RotationComponent.update(new Rotation(yaw, pitch),
                yawSpeed, pitchSpeed,
                MathUtil.random(20, 35), MathUtil.random(20, 30),
                MathUtil.random(0, 2), 15, false);
    }

    public static void rotationDef(LivingEntity target, boolean ray) {
        if (target == null || mc.player == null) return;

        Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(target, true)
                .subtract(mc.player.getEyePos()).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90, 90);

        float yawSpeed = ray ? MathUtil.random(110f, 130f) : MathUtil.random(150f, 190f);
        float pitchSpeed = ray ? MathUtil.random(11f, 19f) : MathUtil.random(21f, 32f);

        RotationComponent.update(new Rotation(yaw, pitch),
                yawSpeed, pitchSpeed,
                MathUtil.random(110, 130), MathUtil.random(90, 110),
                0, 1, false);
    }

    public static void slothOld(LivingEntity target, boolean isAttack) {
        if (target == null || mc.player == null) return;

        Vec3d eyePos = mc.player.getEyePos();
        float addyVact = 0.38F * (float) Math.cos(System.currentTimeMillis() / ThreadLocalRandom.current().nextDouble(250D, 400D));
        float addyVacZ = 0.32F * (float) Math.cos(System.currentTimeMillis() / ThreadLocalRandom.current().nextDouble(220D, 400D));
        float addyVacX = 0.33F * (float) Math.cos(System.currentTimeMillis() / ThreadLocalRandom.current().nextDouble(300D, 400D));

        Vec3d vec = target.getPos()
                .add(addyVacZ, MathHelper.clamp(eyePos.y - target.getY(), 0.0F, 1F + addyVact), addyVacX)
                .subtract(eyePos).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90, 90);

        float yawSpeed = MathUtil.random(30f, 45f);
        float pitchSpeed = MathUtil.random(15f, 25f);

        float yawJitter = isAttack ? MathUtil.random(1, 3) : (float) (25 * Math.sin(System.currentTimeMillis() /  11.5D));
        float pitchJitter = isAttack ? MathUtil.random(3, 4) : (float) (360 * Math.cos(System.currentTimeMillis() / 31.5D));

        RotationComponent.update(new Rotation(yaw + yawJitter, pitch + pitchJitter),
                yawSpeed, pitchSpeed,
                MathUtil.random(20, 30), MathUtil.random(10, 20),
                MathUtil.random(1, 2), 15, false);
    }

    private static float getSens(float delta) {
        return RotationComponent.getSens(delta);
    }

    private static double calculateFovFromCamera(LivingEntity target) {
        if (mc.player == null) return 0.0;
        Rotation targetRot = RotationUtil.rotationAt(RotationUtil.getSpot(target));
        float yawDelta = MathHelper.wrapDegrees(targetRot.getYaw() - mc.player.getYaw());
        float pitchDelta = targetRot.getPitch() - mc.player.getPitch();
        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }
}
