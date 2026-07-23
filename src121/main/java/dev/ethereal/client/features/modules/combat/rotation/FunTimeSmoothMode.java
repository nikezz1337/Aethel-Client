package dev.ethereal.client.features.modules.combat.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.system.interfaces.QuickImports;

import java.security.SecureRandom;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

public class FunTimeSmoothMode extends AngleSmoothMode implements QuickImports {

    private final SecureRandom secureRandom = new SecureRandom();
    private final PerlinNoise perlinYaw;
    private final PerlinNoise perlinPitch;
    private final PerlinNoise perlinSpeed;
    private final PerlinNoise perlinWalk;
    private final PerlinNoise perlinMicrostutter;
    private final PerlinNoise perlinOvershoot;
    private final PerlinNoise perlinProfile;

    private long smoothbackShakeStartMs = -1L;
    private double noiseTimeYaw = 0.0;
    private double noiseTimePitch = 0.0;
    private double noiseTimeSpeed = 0.0;
    private double noiseTimeWalk = 0.0;
    private double noiseTimeMicrostutter = 0.0;
    private double noiseTimeOvershoot = 0.0;
    private double noiseTimeProfile = 0.0;

    private long lastCalculationTime = 0L;
    private long nextCalculationDelay = 0L;

    private GameMode currentGameMode = GameMode.NORMAL;
    private float lastPlayerHealth = 20f;
    private int consecutiveHits = 0;
    private int consecutiveMisses = 0;
    private long lastModeChangeMs = 0L;

    private Vec3d currentWalkTarget = Vec3d.ZERO;
    private long lastWalkChangeMs = 0L;
    private long nextWalkChangeDuration = 0L;

    private float velocityYaw = 0f;
    private float velocityPitch = 0f;
    private float lastYaw = 0f;
    private float lastPitch = 0f;

    private long lastMicrostutterMs = 0L;
    private long microstutterDuration = 0L;
    private boolean inMicrostutter = false;

    private float overshootYaw = 0f;
    private float overshootPitch = 0f;
    private long overshootStartMs = 0L;
    private long overshootDuration = 0L;
    private boolean inOvershoot = false;

    private boolean attackSnapRequested = false;
    private float attackSnapYaw = 0f;
    private float attackSnapPitch = 0f;
    private long attackSnapStartMs = 0L;
    private long attackSnapDuration = 0L;

    private boolean postAttackRecoil = false;
    private float recoilYaw = 0f;
    private float recoilPitch = 0f;
    private long recoilStartMs = 0L;
    private long recoilDuration = 0L;

    private final long sessionStartTime;
    private float profileDrift = 0f;
    private float fatigueLevel = 0f;

    private float baseSpeedMultiplier = 1.0f;
    private float baseTremorMultiplier = 1.0f;
    private float baseInertiaMultiplier = 1.0f;
    private long lastProfileUpdateMs = 0L;
    private static final long PROFILE_UPDATE_INTERVAL = 180000L;

    private final Supplier<Boolean> canAttackSupplier;
    private final Supplier<Integer> attackCountSupplier;
    private final Supplier<Long> elapsedMsSupplier;
    private final LongPredicate timerFinished;

    private enum GameMode {
        NORMAL,
        PANIC,
        CONFIDENCE,
        FOCUS
    }

    public FunTimeSmoothMode(Supplier<Boolean> canAttackSupplier,
                             Supplier<Integer> attackCountSupplier,
                             Supplier<Long> elapsedMsSupplier,
                             LongPredicate timerFinished) {
        super("FunTime");
        this.canAttackSupplier = canAttackSupplier;
        this.attackCountSupplier = attackCountSupplier;
        this.elapsedMsSupplier = elapsedMsSupplier;
        this.timerFinished = timerFinished;

        long baseSeed = generateEnvironmentBasedSeed();
        this.perlinYaw = new PerlinNoise(baseSeed);
        this.perlinPitch = new PerlinNoise(baseSeed + 1337);
        this.perlinSpeed = new PerlinNoise(baseSeed + 9999);
        this.perlinWalk = new PerlinNoise(baseSeed + 42069);
        this.perlinMicrostutter = new PerlinNoise(baseSeed + 13337);
        this.perlinOvershoot = new PerlinNoise(baseSeed + 99999);
        this.perlinProfile = new PerlinNoise(baseSeed + 777777);

        this.lastYaw = mc.player != null ? mc.player.getYaw() : 0f;
        this.lastPitch = mc.player != null ? mc.player.getPitch() : 0f;

        this.sessionStartTime = System.currentTimeMillis();
        this.lastProfileUpdateMs = sessionStartTime;
        this.lastCalculationTime = sessionStartTime;
        this.lastModeChangeMs = sessionStartTime;

        updateDynamicProfile(sessionStartTime);

        nextCalculationDelay = secureRandom.nextInt(3) + 1;
    }

    private long generateEnvironmentBasedSeed() {
        long seed = System.nanoTime();

        if (mc.player != null && mc.world != null) {
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
                int ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
                seed ^= (long) ping * 31337L;
            }

            int fps = mc.getCurrentFps();
            seed ^= (long) fps * 99991L;

            int nearbyEntities = 0;
            for (Entity entity : mc.world.getEntities()) {
                if (entity != null && entity.squaredDistanceTo(mc.player) <= 400.0) {
                    nearbyEntities++;
                }
            }
            seed ^= (long) nearbyEntities * 73939L;

            seed ^= (long) (mc.player.getX() * 1000) * 12345L;
            seed ^= (long) (mc.player.getZ() * 1000) * 67890L;
        }

        seed ^= secureRandom.nextLong();

        return seed;
    }

    public float getTremor(float intensity) {
        return 0;
    }

    public void onAttack() {
        attackSnapRequested = true;
        attackSnapStartMs = System.currentTimeMillis();
        attackSnapDuration = 40L + secureRandom.nextInt(30);

        float snapMagnitude = 2f + secureRandom.nextFloat() * 7f;
        attackSnapYaw = (secureRandom.nextBoolean() ? 1f : -1f) * snapMagnitude;
        attackSnapPitch = (secureRandom.nextBoolean() ? 1f : -1f) * snapMagnitude * 0.4f;

        postAttackRecoil = true;
        recoilStartMs = System.currentTimeMillis();
        recoilDuration = 60L + secureRandom.nextInt(180);

        float recoilMagnitude = 3f + secureRandom.nextFloat() * 10f;
        recoilYaw = (secureRandom.nextBoolean() ? 1f : -1f) * recoilMagnitude;
        recoilPitch = (secureRandom.nextBoolean() ? 1f : -1f) * recoilMagnitude * 0.5f;

        consecutiveHits++;
        consecutiveMisses = 0;
    }

    public void onMiss() {
        consecutiveMisses++;
        consecutiveHits = 0;
    }

    private void updateGameMode(Entity target, long currentTime) {
        if (mc.player == null) return;

        if (currentTime - lastModeChangeMs < 2000L) {
            return;
        }

        float currentHealth = mc.player.getHealth();
        GameMode newMode = GameMode.NORMAL;

        if (currentHealth < 8f && currentHealth < lastPlayerHealth) {
            newMode = GameMode.PANIC;
        }
        else if (consecutiveHits >= 3) {
            newMode = GameMode.CONFIDENCE;
        }
        else if (target instanceof LivingEntity living) {
            Vec3d velocity = living.getVelocity();
            double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (speed > 0.15) {
                newMode = GameMode.FOCUS;
            }
        }

        if (newMode != currentGameMode) {
            currentGameMode = newMode;
            lastModeChangeMs = currentTime;
        }

        lastPlayerHealth = currentHealth;
    }

    private float getGameModeSpeedMultiplier() {
        return switch (currentGameMode) {
            case PANIC -> 1.3f;
            case CONFIDENCE -> 0.75f;
            case FOCUS -> 1.1f;
            default -> 1.0f;
        };
    }

    private float getGameModeTremorMultiplier() {
        return switch (currentGameMode) {
            case PANIC -> 1.6f;
            case CONFIDENCE -> 0.7f;
            case FOCUS -> 1.2f;
            default -> 1.0f;
        };
    }

    private float getGameModeCorrectionMultiplier() {
        return switch (currentGameMode) {
            case PANIC -> 1.4f;
            case CONFIDENCE -> 0.8f;
            case FOCUS -> 1.5f;
            default -> 1.0f;
        };
    }

    private void updateDynamicProfile(long currentTime) {
        float sessionMinutes = (currentTime - sessionStartTime) / 60000f;

        noiseTimeProfile += 0.1;
        double fatigueNoise = perlinProfile.octaveNoise(noiseTimeProfile, 0, 2, 0.5);
        fatigueLevel = MathHelper.clamp(sessionMinutes / 120f + (float) fatigueNoise * 0.2f, 0f, 0.4f);

        profileDrift += (float) (secureRandom.nextGaussian() * 0.05f);
        profileDrift = MathHelper.clamp(profileDrift, -0.3f, 0.3f);

        double speedNoise = perlinProfile.octaveNoise(noiseTimeProfile + 10, 0, 2, 0.5);
        double tremorNoise = perlinProfile.octaveNoise(noiseTimeProfile + 20, 0, 2, 0.5);
        double inertiaNoise = perlinProfile.octaveNoise(noiseTimeProfile + 30, 0, 2, 0.5);

        baseSpeedMultiplier = 0.85f + (float) speedNoise * 0.3f - fatigueLevel;
        baseTremorMultiplier = 0.85f + (float) tremorNoise * 0.3f + fatigueLevel * 0.5f;
        baseInertiaMultiplier = 0.85f + (float) inertiaNoise * 0.3f + fatigueLevel * 0.3f;

        baseSpeedMultiplier = MathHelper.clamp(baseSpeedMultiplier, 0.6f, 1.2f);
        baseTremorMultiplier = MathHelper.clamp(baseTremorMultiplier, 0.7f, 1.4f);
        baseInertiaMultiplier = MathHelper.clamp(baseInertiaMultiplier, 0.8f, 1.3f);

        lastProfileUpdateMs = currentTime;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        boolean canAttack = canAttackSupplier.get();
        int count = attackCountSupplier.get();
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCalculationTime < nextCalculationDelay) {
            return currentAngle;
        }
        lastCalculationTime = currentTime;
        nextCalculationDelay = secureRandom.nextInt(3) + 1;

        updateGameMode(entity, currentTime);

        if (currentTime - lastProfileUpdateMs >= PROFILE_UPDATE_INTERVAL) {
            updateDynamicProfile(currentTime);
        }

        if (entity != null) {
            this.smoothbackShakeStartMs = -1L;

            return humanAimToTarget(currentAngle, targetAngle, entity, currentTime);
        }

        return humanReturnToPlayer(currentAngle, entity, count, currentTime);
    }

    private Angle humanAimToTarget(Angle currentAngle, Angle targetAngle, Entity entity, long currentTime) {
        updateRandomWalkTarget(entity, currentTime);

        Angle walkAdjustedTarget = applyRandomWalk(targetAngle, entity);

        Angle deltaTurns = AngleUtil.calculateDelta(currentAngle, walkAdjustedTarget);
        float yawDelta = deltaTurns.getYaw();
        float pitchDelta = deltaTurns.getPitch();
        float totalDelta = (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);

        if (totalDelta < 0.01F) return currentAngle;

        if (attackSnapRequested && currentTime - attackSnapStartMs < attackSnapDuration) {
            yawDelta += attackSnapYaw;
            pitchDelta += attackSnapPitch;
        } else if (attackSnapRequested) {
            attackSnapRequested = false;
        }

        if (postAttackRecoil) {
            long recoilElapsed = currentTime - recoilStartMs;
            if (recoilElapsed < recoilDuration) {
                float recoilProgress = (float) recoilElapsed / (float) recoilDuration;
                float recoilFade = 1.0f - recoilProgress;
                recoilFade = recoilFade * recoilFade;

                yawDelta += recoilYaw * recoilFade;
                pitchDelta += recoilPitch * recoilFade;
            } else {
                postAttackRecoil = false;
            }
        }

        updateMicrostutter(currentTime, totalDelta);
        if (inMicrostutter) {
            velocityYaw *= 0.15f;
            velocityPitch *= 0.15f;

            noiseTimeYaw += 0.04;
            noiseTimePitch += 0.03;
            double tremorYaw = perlinYaw.octaveNoise(noiseTimeYaw, 0, 4, 0.6) * 0.8;
            double tremorPitch = perlinPitch.octaveNoise(noiseTimePitch, 0, 4, 0.6) * 0.5;

            float rawYaw = currentAngle.getYaw() + velocityYaw + (float) tremorYaw;
            float rawPitch = currentAngle.getPitch() + velocityPitch + (float) tremorPitch;

            float gcd = getGCDValue();
            float newYaw = currentAngle.getYaw() + Math.round((rawYaw - currentAngle.getYaw()) / gcd) * gcd;
            float newPitch = currentAngle.getPitch() + Math.round((rawPitch - currentAngle.getPitch()) / gcd) * gcd;
            newPitch = MathHelper.clamp(newPitch, -90f, 90f);

            return new Angle(newYaw, newPitch);
        }

        updateOvershoot(currentTime, totalDelta);
        if (inOvershoot) {
            yawDelta += overshootYaw * 0.7f;
            pitchDelta += overshootPitch * 0.7f;
        }

        noiseTimeSpeed += 0.006 + secureRandom.nextDouble() * 0.003;
        double speedNoise = perlinSpeed.octaveNoise(noiseTimeSpeed, 0, 3, 0.5);
        float humanSpeed = (0.8f + (float) speedNoise * 0.7f) * baseSpeedMultiplier * getGameModeSpeedMultiplier();

        float baseLimitYaw = (75f + profileDrift * 20f) + (float) (secureRandom.nextGaussian() * 20f);
        float baseLimitPitch = (65f + profileDrift * 15f) + (float) (secureRandom.nextGaussian() * 18f);

        float yawLimit = Math.abs(yawDelta / totalDelta) * baseLimitYaw;
        float pitchLimit = Math.abs(pitchDelta / totalDelta) * baseLimitPitch;

        float targetYawVelocity = MathHelper.clamp(yawDelta, -yawLimit, yawLimit) * humanSpeed;
        float targetPitchVelocity = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit) * humanSpeed;

        float inertia = (0.45f + (float) (secureRandom.nextGaussian() * 0.12f)) * baseInertiaMultiplier;
        inertia = MathHelper.clamp(inertia, 0.4f, 0.9f);
        velocityYaw = velocityYaw * inertia + targetYawVelocity * (1f - inertia);
        velocityPitch = velocityPitch * inertia + targetPitchVelocity * (1f - inertia);

        noiseTimeYaw += (0.12 + secureRandom.nextDouble() * 0.08) * getGameModeCorrectionMultiplier();
        noiseTimePitch += (0.10 + secureRandom.nextDouble() * 0.06) * getGameModeCorrectionMultiplier();

        double tremorYaw = perlinYaw.octaveNoise(noiseTimeYaw, 0, 4, 0.6) * 0.7 * baseTremorMultiplier * getGameModeTremorMultiplier();
        double tremorPitch = perlinPitch.octaveNoise(noiseTimePitch, 0, 4, 0.6) * 0.5 * baseTremorMultiplier * getGameModeTremorMultiplier();

        float movementIntensity = Math.min(totalDelta / 45f, 1f);
        tremorYaw *= (1f + movementIntensity * 0.4f);
        tremorPitch *= (1f + movementIntensity * 0.3f);

        float rawYaw = currentAngle.getYaw() + velocityYaw + (float) tremorYaw;
        float rawPitch = currentAngle.getPitch() + velocityPitch + (float) tremorPitch;

        float gcd = getGCDValue();
        float newYaw = currentAngle.getYaw() + Math.round((rawYaw - currentAngle.getYaw()) / gcd) * gcd;
        float newPitch = currentAngle.getPitch() + Math.round((rawPitch - currentAngle.getPitch()) / gcd) * gcd;
        newPitch = MathHelper.clamp(newPitch, -90f, 90f);

        float actualYawMove = newYaw - currentAngle.getYaw();
        float actualPitchMove = newPitch - currentAngle.getPitch();

        velocityYaw = actualYawMove - (float) tremorYaw;
        velocityPitch = actualPitchMove - (float) tremorPitch;

        lastYaw = newYaw;
        lastPitch = newPitch;

        return new Angle(newYaw, newPitch);
    }

    private void updateMicrostutter(long currentTime, float totalDelta) {
        if (inMicrostutter) {
            if (currentTime - lastMicrostutterMs >= microstutterDuration) {
                inMicrostutter = false;
            }
            return;
        }

        noiseTimeMicrostutter += 0.02;
        double stutterNoise = perlinMicrostutter.octaveNoise(noiseTimeMicrostutter, 0, 2, 0.5);

        float stutterChance = 0.015f + (totalDelta / 100f) * 0.03f;

        if (stutterNoise > (1.0 - stutterChance)) {
            inMicrostutter = true;
            lastMicrostutterMs = currentTime;
            microstutterDuration = 20L + secureRandom.nextInt(60);
        }
    }

    private void updateOvershoot(long currentTime, float totalDelta) {
        if (inOvershoot) {
            float progress = (float) (currentTime - overshootStartMs) / (float) overshootDuration;
            if (progress >= 1.0f) {
                inOvershoot = false;
                overshootYaw = 0f;
                overshootPitch = 0f;
            } else {
                float fade = 1.0f - progress;
                fade = fade * fade;
                overshootYaw *= fade;
                overshootPitch *= fade;
            }
            return;
        }

        noiseTimeOvershoot += 0.015;
        double overshootNoise = perlinOvershoot.octaveNoise(noiseTimeOvershoot, 0, 2, 0.5);

        float overshootChance = 0.02f + (totalDelta / 80f) * 0.05f;

        if (overshootNoise > (1.0 - overshootChance) && totalDelta > 10f) {
            inOvershoot = true;
            overshootStartMs = currentTime;
            overshootDuration = 100L + secureRandom.nextInt(150);

            float overshootMagnitude = 0.25f + secureRandom.nextFloat() * 1.5f;
            overshootYaw = (secureRandom.nextBoolean() ? 1f : -1f) * overshootMagnitude;
            overshootPitch = (secureRandom.nextBoolean() ? 1f : -1f) * overshootMagnitude * 0.6f;
        }
    }

    private float getGCDValue() {
        if (mc.options == null) return 1.0f;
        float sensitivity = (float) (double) mc.options.getMouseSensitivity().getValue();
        float f = sensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 8.0f;
        return gcd * 0.15f;
    }

    private void updateRandomWalkTarget(Entity entity, long currentTime) {
        if (currentTime - lastWalkChangeMs < nextWalkChangeDuration) {
            return;
        }

        nextWalkChangeDuration = 80L + secureRandom.nextInt(270);
        lastWalkChangeMs = currentTime;

        if (!(entity instanceof LivingEntity living)) {
            currentWalkTarget = Vec3d.ZERO;
            return;
        }

        Box box = living.getBoundingBox();
        double width = box.maxX - box.minX;
        double height = box.maxY - box.minY;

        noiseTimeWalk += 0.5;
        double walkNoiseX = perlinWalk.octaveNoise(noiseTimeWalk, 0, 2, 0.5);
        double walkNoiseY = perlinWalk.octaveNoise(0, noiseTimeWalk, 2, 0.5);

        double offsetX = walkNoiseX * width * 0.35;
        double offsetY = height * 0.6 + walkNoiseY * height * 0.25;

        currentWalkTarget = new Vec3d(offsetX, offsetY, 0);
    }

    private Angle applyRandomWalk(Angle targetAngle, Entity entity) {
        if (currentWalkTarget.equals(Vec3d.ZERO) || mc.player == null) {
            return targetAngle;
        }

        if (!(entity instanceof LivingEntity living)) {
            return targetAngle;
        }

        Vec3d entityPos = living.getPos();
        Vec3d walkPoint = entityPos.add(currentWalkTarget.x, currentWalkTarget.y, currentWalkTarget.z);

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = walkPoint.subtract(eyePos).normalize();

        double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0;
        double pitch = -Math.toDegrees(Math.asin(direction.y));

        return new Angle((float) yaw, (float) pitch);
    }

    private Angle humanReturnToPlayer(Angle currentAngle, Entity entity, int count, long currentTime) {
        Angle playerTurns = AngleUtil.cameraAngle();
        Angle returnDelta = AngleUtil.calculateDelta(currentAngle, playerTurns);

        float retYaw = returnDelta.getYaw();
        float retPitch = returnDelta.getPitch();
        float retTotal = (float) Math.sqrt(retYaw * retYaw + retPitch * retPitch);

        noiseTimeYaw += 0.04 + secureRandom.nextDouble() * 0.02;
        noiseTimePitch += 0.03 + secureRandom.nextDouble() * 0.015;

        double shakeNoiseYaw = perlinYaw.octaveNoise(noiseTimeYaw, 0, 3, 0.5);
        double shakeNoisePitch = perlinPitch.octaveNoise(noiseTimePitch, 0, 3, 0.5);

        float shakeYaw = (float) (shakeNoiseYaw * 8f + secureRandom.nextGaussian() * 1.5f);
        float shakePitch = (float) (shakeNoisePitch * 5f + secureRandom.nextGaussian() * 1f);

        if (entity != null) {
            this.smoothbackShakeStartMs = -1L;
        } else {
            if (this.smoothbackShakeStartMs < 0L) {
                this.smoothbackShakeStartMs = currentTime;
            }

            float fadeTime = (float) (currentTime - this.smoothbackShakeStartMs);
            float fadeRatio = 1.0F - MathHelper.clamp(fadeTime / 1500.0F, 0.0F, 1.0F);
            fadeRatio = fadeRatio * fadeRatio * fadeRatio;

            shakeYaw *= fadeRatio;
            shakePitch *= fadeRatio;
        }

        noiseTimeSpeed += 0.01;
        double limitNoise = perlinSpeed.octaveNoise(noiseTimeSpeed, 0, 2, 0.5);

        float baseLimitMultiplier;
        if (!timerFinished.test(535)) {
            baseLimitMultiplier = 0.0F;
        } else {
            baseLimitMultiplier = 20f + (float) (limitNoise * 40f);
            if (secureRandom.nextFloat() < 0.1f) {
                baseLimitMultiplier *= 1.5f;
            }
        }

        float yawLimit = retTotal > 0 ? Math.abs(retYaw / retTotal) * baseLimitMultiplier : 0.0F;
        float pitchLimit = retTotal > 0 ? Math.abs(retPitch / retTotal) * baseLimitMultiplier : 0.0F;

        float returnSpeed = 0.5f + secureRandom.nextFloat() * 0.3f;
        velocityYaw *= 0.85f;
        velocityPitch *= 0.85f;

        if (secureRandom.nextFloat() < 0.08f) {
            returnSpeed *= 0.4f;
        }

        velocityYaw += MathHelper.clamp(retYaw, -yawLimit, yawLimit) * returnSpeed * 0.25f;
        velocityPitch += MathHelper.clamp(retPitch, -pitchLimit, pitchLimit) * returnSpeed * 0.25f;

        float rawYaw = currentAngle.getYaw() + velocityYaw + shakeYaw;
        float rawPitch = currentAngle.getPitch() + velocityPitch + shakePitch;

        float gcd = getGCDValue();
        float newYaw = currentAngle.getYaw() + Math.round((rawYaw - currentAngle.getYaw()) / gcd) * gcd;
        float newPitch = currentAngle.getPitch() + Math.round((rawPitch - currentAngle.getPitch()) / gcd) * gcd;
        newPitch = MathHelper.clamp(newPitch, -90f, 90f);

        float actualYawMove = newYaw - currentAngle.getYaw();
        float actualPitchMove = newPitch - currentAngle.getPitch();

        velocityYaw = actualYawMove - shakeYaw;
        velocityPitch = actualPitchMove - shakePitch;

        return new Angle(newYaw, newPitch);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
