package antileak.base.client.modules.impl.combat.components.rotations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import antileak.base.api.QClient;
import antileak.base.api.storages.implement.RotationStorage;
import antileak.base.api.utils.rotate.Rotation;
import antileak.base.api.utils.rotate.RotationUtils;
import antileak.base.client.modules.impl.combat.Aura;
import antileak.base.client.modules.impl.combat.components.RotationsSystem;
import antileak.base.client.modules.impl.combat.components.gcd.GCDUtil;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestRotation extends RotationsSystem implements QClient {

    private static final Path DATASET_PATH = Path.of(System.getProperty("user.home"), "Desktop", "data.json");

    private final List<DatasetFrame> frames = new ArrayList<>();

    private LivingEntity trackedTarget;
    private LivingEntity trackedRotationTarget;
    private Vec3d currentAimPoint;
    private Vec3d targetAimPoint;

    private long lastModified = Long.MIN_VALUE;
    private long lastLoadAttempt;
    private boolean datasetReady;

    private int playbackIndex;
    private int aimPointTicks;
    private int aimPointRefreshTicks;
    private int smoothProfileTicks;

    private float smoothYawStep;
    private float smoothPitchStep;
    private float smoothYaw;
    private float smoothPitch;
    private float yawSmoothFactor = 1.0F;
    private float pitchSmoothFactor = 1.0F;

    private boolean hasRotationState;

    public void reset() {
        trackedTarget = null;
        trackedRotationTarget = null;
        currentAimPoint = null;
        targetAimPoint = null;
        playbackIndex = 0;
        aimPointTicks = 0;
        aimPointRefreshTicks = 0;
        smoothProfileTicks = 0;
        smoothYawStep = 0.0F;
        smoothPitchStep = 0.0F;
        smoothYaw = 0.0F;
        smoothPitch = 0.0F;
        yawSmoothFactor = 1.0F;
        pitchSmoothFactor = 1.0F;
        hasRotationState = false;
    }

    
    @Override
    public void updateRotations(LivingEntity target) {
        if (mc.player == null || target == null) {
            return;
        }

        boolean focus = shouldFocus();
        ensureDatasetLoaded();
        Vec3d aimPoint = selectAimPoint(target, focus);
        Vec2f rot = RotationUtils.getRotations(aimPoint);

        if (!datasetReady || frames.isEmpty()) {
            RotationStorage.update(
                    new Rotation(rot.x, MathHelper.clamp(rot.y, -89.0F, 89.0F)),
                    360.0F, 360.0F, 45.0F, 45.0F,
                    0, 1, Aura.clientLook.isState()
            );
            return;
        }

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        syncRotationState(target, currentYaw, currentPitch);

        float remainingYaw = MathHelper.wrapDegrees(rot.x - smoothYaw);
        float remainingPitch = rot.y - smoothPitch;

        DatasetFrame frame = pickFrame(remainingYaw, remainingPitch, focus);
        updateSmoothProfile(frame, remainingYaw, remainingPitch, focus);

        float gcd = Math.max(GCDUtil.getGCDValue(), 0.0001F);
        float yawStep = buildAxisStep(remainingYaw, frame, true, focus);
        float pitchStep = buildAxisStep(remainingPitch, frame, false, focus);

        yawStep += buildJitter(frame, remainingYaw, true, gcd);
        pitchStep += buildJitter(frame, remainingPitch, false, gcd);

        smoothYawStep = smoothAxisStep(smoothYawStep, yawStep, remainingYaw, true, focus);
        smoothPitchStep = smoothAxisStep(smoothPitchStep, pitchStep, remainingPitch, false, focus);

        float quantizedYawStep = quantizeDelta(smoothYawStep, remainingYaw, gcd, true);
        float quantizedPitchStep = quantizeDelta(smoothPitchStep, remainingPitch, gcd, false);

        smoothYaw = MathHelper.wrapDegrees(smoothYaw + quantizedYawStep);
        smoothPitch = MathHelper.clamp(smoothPitch + quantizedPitchStep, -89.0F, 89.0F);

        RotationStorage.update(
                new Rotation(smoothYaw, smoothPitch),
                360.0F, 360.0F, 45.0F, 45.0F,
                0, 1, Aura.clientLook.isState()
        );
    }

    private boolean shouldFocus() {
        float cooldown = mc.player.getAttackCooldownProgress(1.5F);
        boolean fallingForCrit = !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0.0
                && mc.player.fallDistance > 0.0F;
        return cooldown >= 0.88F || fallingForCrit;
    }

    private void ensureDatasetLoaded() {
        long now = System.currentTimeMillis();
        if (!shouldReload(now)) {
            return;
        }

        lastLoadAttempt = now;
        long modified = readLastModified();
        if (datasetReady && modified == lastModified) {
            return;
        }

        frames.clear();
        datasetReady = false;

        if (!Files.exists(DATASET_PATH)) {
            lastModified = Long.MIN_VALUE;
            return;
        }

        try (Reader reader = Files.newBufferedReader(DATASET_PATH)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }

                DatasetFrame frame = parseFrame(element.getAsJsonObject());
                if (frame != null) {
                    frames.add(frame);
                }
            }

            datasetReady = !frames.isEmpty();
            lastModified = modified;
            reset();
        } catch (IOException | IllegalStateException ignored) {
            datasetReady = false;
            lastModified = Long.MIN_VALUE;
            reset();
        }
    }

    private boolean shouldReload(long now) {
        if (!datasetReady || frames.isEmpty()) {
            return now - lastLoadAttempt >= 1500L;
        }
        return now - lastLoadAttempt >= 3000L;
    }

    private long readLastModified() {
        try {
            return Files.exists(DATASET_PATH) ? Files.getLastModifiedTime(DATASET_PATH).toMillis() : Long.MIN_VALUE;
        } catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private DatasetFrame parseFrame(JsonObject object) {
        float fromYaw = getFloat(object, "fromYaw");
        float toYaw = getFloat(object, "toYaw");
        float fromPitch = getFloat(object, "fromPitch");
        float toPitch = getFloat(object, "toPitch");

        float signedYaw = MathHelper.wrapDegrees(toYaw - fromYaw);
        float signedPitch = toPitch - fromPitch;
        float absYaw = Math.abs(signedYaw);
        float absPitch = Math.abs(signedPitch);

        float deltaYaw = Math.max(getFloat(object, "deltaYaw"), absYaw);
        float deltaPitch = Math.max(getFloat(object, "deltaPitch"), absPitch);
        if (deltaYaw <= 0.0F && deltaPitch <= 0.0F) {
            return null;
        }

        DatasetFrame frame = new DatasetFrame();
        frame.deltaYaw = deltaYaw;
        frame.deltaPitch = deltaPitch;
        frame.signedYaw = signedYaw != 0.0F
                ? signedYaw
                : Math.signum(getFloat(object, "jitterYawDir")) * deltaYaw;
        frame.signedPitch = signedPitch != 0.0F
                ? signedPitch
                : Math.signum(getFloat(object, "jitterPitchDir")) * deltaPitch;
        frame.rotationSpeed = Math.max(getFloat(object, "rotationSpeed"), 0.0F);
        frame.jitterScore = Math.max(getFloat(object, "jitterScore"), 0.0F);
        frame.jitterYawSpeed = Math.max(getFloat(object, "jitterYawSpeed"), 0.0F);
        frame.jitterPitchSpeed = Math.max(getFloat(object, "jitterPitchSpeed"), 0.0F);
        frame.isJittering = getBoolean(object, "isJittering");
        frame.attacking = getBoolean(object, "attacking");
        frame.combatFrame = getBoolean(object, "isCombatFrame");
        frame.instantSnap = getBoolean(object, "isInstantSnap");
        frame.timeDeltaMs = Math.max(1L, object.has("timeDeltaMs") ? object.get("timeDeltaMs").getAsLong() : 50L);
        return frame;
    }

    private DatasetFrame pickFrame(float remainingYaw, float remainingPitch, boolean focus) {
        float pressure = Math.abs(remainingYaw) + Math.abs(remainingPitch) * 0.82F;
        int size = frames.size();
        int window = Math.min(size, focus ? 78 : 56);
        int bestIndex = playbackIndex % size;
        float bestScore = Float.MAX_VALUE;

        for (int i = 0; i < window; i++) {
            int index = (playbackIndex + i) % size;
            DatasetFrame frame = frames.get(index);
            float framePressure = frame.deltaYaw + frame.deltaPitch * 0.82F;
            float score = Math.abs(framePressure - pressure);

            if (focus) {
                if (!frame.isCombatLike()) {
                    score += 3.0F;
                }
                if (frame.instantSnap) {
                    score -= 0.5F;
                }
            } else if (frame.isCombatLike()) {
                score += 1.6F;
            }

            if (pressure < 10.0F && frame.isJittering) {
                score -= Math.min(frame.jitterScore, 2.6F) * 0.20F;
            }

            score += i * 0.032F;
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }

        playbackIndex = (bestIndex + 1) % size;
        return frames.get(bestIndex);
    }

    private void updateSmoothProfile(DatasetFrame frame, float remainingYaw, float remainingPitch, boolean focus) {
        if (smoothProfileTicks > 0) {
            smoothProfileTicks--;
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        float pressure = MathHelper.clamp((Math.abs(remainingYaw) + Math.abs(remainingPitch)) / 32.0F, 0.0F, 1.0F);
        float timePressure = MathHelper.clamp(frame.timeDeltaMs / 120.0F, 0.0F, 1.0F);

        float yawMin = focus ? 0.94F : 0.86F;
        float yawMax = focus ? 1.12F : 1.04F;
        float pitchMin = focus ? 0.92F : 0.84F;
        float pitchMax = focus ? 1.08F : 1.00F;

        yawSmoothFactor = random.nextFloat(yawMin, yawMax + pressure * 0.08F + timePressure * 0.04F);
        pitchSmoothFactor = random.nextFloat(pitchMin, pitchMax + pressure * 0.06F + timePressure * 0.03F);

        if (frame.isCombatLike()) {
            yawSmoothFactor *= 1.02F;
            pitchSmoothFactor *= 1.015F;
        }

        smoothProfileTicks = random.nextInt(focus ? 2 : 3, focus ? 6 : 8);
    }

    private float buildAxisStep(float remaining, DatasetFrame frame, boolean yawAxis, boolean focus) {
        float desiredAbs = Math.abs(remaining);
        if (desiredAbs <= 0.0001F) {
            return 0.0F;
        }

        float template = yawAxis ? frame.deltaYaw : frame.deltaPitch;
        float speedBoost = 0.30F + MathHelper.clamp(frame.rotationSpeed * (yawAxis ? 3.4F : 2.8F), 0.0F, yawAxis ? 0.20F : 0.16F);
        float pressureBoost = MathHelper.clamp(desiredAbs / (yawAxis ? 105.0F : 82.0F), 0.09F, yawAxis ? 0.52F : 0.46F);
        float step = Math.max(template * Math.max(speedBoost, pressureBoost), yawAxis ? 0.03F : 0.024F);

        if (frame.instantSnap) {
            step = Math.max(step, desiredAbs * (yawAxis ? 0.085F : 0.065F));
        }

        if (frame.attacking || frame.combatFrame) {
            step *= yawAxis ? 1.02F : 1.015F;
        }

        step *= yawAxis ? 0.50F : 0.46F;

        float finishThreshold = yawAxis ? 6.0F : 4.0F;
        if (desiredAbs < finishThreshold) {
            float finishBoost = 1.0F + (finishThreshold - desiredAbs) / finishThreshold * 0.18F;
            step *= finishBoost;
        }

        float maxStep = yawAxis
                ? Math.max(0.48F, desiredAbs * (frame.instantSnap ? 0.11F : 0.065F))
                : Math.max(0.34F, desiredAbs * (frame.instantSnap ? 0.09F : 0.058F));
        step = Math.min(step, maxStep);
        step = Math.min(step, desiredAbs);
        return Math.signum(remaining) * step;
    }

    private float buildJitter(DatasetFrame frame, float remaining, boolean yawAxis, float gcd) {
        float desiredAbs = Math.abs(remaining);
        if (desiredAbs > (yawAxis ? 6.5F : 4.8F)) {
            return 0.0F;
        }

        float speed = yawAxis ? frame.jitterYawSpeed : frame.jitterPitchSpeed;
        float base = gcd * MathHelper.clamp(frame.jitterScore * 0.010F, 0.0F, yawAxis ? 0.15F : 0.11F);
        base += gcd * MathHelper.clamp(speed * (yawAxis ? 1.3F : 1.0F), 0.0F, yawAxis ? 0.10F : 0.07F);

        if (frame.isJittering) {
            base *= 1.05F;
        }

        if (base <= 0.0F) {
            return 0.0F;
        }

        float direction = ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F;
        float jitter = base * ThreadLocalRandom.current().nextFloat(0.30F, 0.95F) * direction;
        if (Math.abs(jitter) > desiredAbs && Math.signum(jitter) == Math.signum(remaining)) {
            jitter = remaining;
        }

        return jitter;
    }

    private void syncRotationState(LivingEntity target, float currentYaw, float currentPitch) {
        if (!hasRotationState || trackedRotationTarget != target) {
            trackedRotationTarget = target;
            smoothYaw = currentYaw;
            smoothPitch = currentPitch;
            smoothYawStep = 0.0F;
            smoothPitchStep = 0.0F;
            yawSmoothFactor = 1.0F;
            pitchSmoothFactor = 1.0F;
            smoothProfileTicks = 0;
            hasRotationState = true;
        }
    }

    private float smoothAxisStep(float currentStep, float desiredStep, float remaining, boolean yawAxis, boolean focus) {
        float desiredAbs = Math.abs(remaining);
        if (desiredAbs <= 0.0001F) {
            return 0.0F;
        }

        float baseAlpha = yawAxis
                ? (focus ? 0.092F : 0.060F)
                : (focus ? 0.082F : 0.055F);
        float alpha = baseAlpha * (yawAxis ? yawSmoothFactor : pitchSmoothFactor);
        float smoothed = currentStep + (desiredStep - currentStep) * MathHelper.clamp(alpha, 0.025F, 0.16F);

        float minCap = yawAxis ? 0.13F : 0.10F;
        float capScale = yawAxis
                ? (focus ? 0.056F : 0.036F)
                : (focus ? 0.046F : 0.032F);
        float randomFactor = yawAxis ? yawSmoothFactor : pitchSmoothFactor;
        float maxCap = minCap + desiredAbs * capScale * MathHelper.clamp(randomFactor, 0.88F, 1.18F);

        float finishThreshold = yawAxis ? 5.5F : 3.8F;
        if (desiredAbs < finishThreshold) {
            maxCap *= 1.12F;
        }

        smoothed = MathHelper.clamp(smoothed, -maxCap, maxCap);
        if (Math.abs(remaining) < Math.abs(smoothed) && Math.signum(remaining) == Math.signum(smoothed)) {
            smoothed = remaining;
        }

        return smoothed;
    }

    private float quantizeDelta(float wantedDelta, float remaining, float gcd, boolean yawAxis) {
        float limited = wantedDelta;
        if (Math.abs(remaining) < Math.abs(limited) && Math.signum(remaining) == Math.signum(limited)) {
            limited = remaining;
        }

        float quantized = Math.round(limited / gcd) * gcd;
        if (quantized == 0.0F && Math.abs(limited) >= gcd * 0.20F) {
            quantized = Math.signum(limited) * gcd;
        }

        if (Math.abs(remaining) < Math.abs(quantized) && Math.signum(remaining) == Math.signum(quantized)) {
            quantized = remaining;
        }

        if (!yawAxis) {
            quantized = MathHelper.clamp(quantized, -89.0F, 89.0F);
        }
        return quantized;
    }

    
    private Vec3d selectAimPoint(LivingEntity target, boolean focus) {
        if (trackedTarget != target || currentAimPoint == null || targetAimPoint == null) {
            trackedTarget = target;
            targetAimPoint = createAimPoint(target, focus);
            currentAimPoint = targetAimPoint;
            aimPointTicks = 0;
            aimPointRefreshTicks = randomRefreshTicks(focus);
            return currentAimPoint;
        }

        if (aimPointTicks++ >= aimPointRefreshTicks) {
            targetAimPoint = createAimPoint(target, focus);
            aimPointTicks = 0;
            aimPointRefreshTicks = randomRefreshTicks(focus);
        }

        float lerp = focus ? 0.060F : 0.040F;
        currentAimPoint = new Vec3d(
                MathHelper.lerp(lerp, currentAimPoint.x, targetAimPoint.x),
                MathHelper.lerp(lerp, currentAimPoint.y, targetAimPoint.y),
                MathHelper.lerp(lerp, currentAimPoint.z, targetAimPoint.z)
        );
        return currentAimPoint;
    }

    private int randomRefreshTicks(boolean focus) {
        return ThreadLocalRandom.current().nextInt(focus ? 7 : 10, focus ? 13 : 18);
    }

    private Vec3d createAimPoint(LivingEntity target, boolean focus) {
        Box box = getPredictedBox(target);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x = MathHelper.lerp(random.nextDouble(0.45D, 0.55D), box.minX, box.maxX);
        double y = MathHelper.lerp(random.nextDouble(focus ? 0.53D : 0.49D, focus ? 0.70D : 0.76D), box.minY, box.maxY);
        double z = MathHelper.lerp(random.nextDouble(0.45D, 0.55D), box.minZ, box.maxZ);
        return new Vec3d(x, y, z);
    }

    private float getFloat(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsFloat() : 0.0F;
    }

    private boolean getBoolean(JsonObject object, String key) {
        return object.has(key) && object.get(key).getAsBoolean();
    }

    private static class DatasetFrame {
        float deltaYaw;
        float deltaPitch;
        float signedYaw;
        float signedPitch;
        float rotationSpeed;
        float jitterScore;
        float jitterYawSpeed;
        float jitterPitchSpeed;
        long timeDeltaMs;
        boolean isJittering;
        boolean attacking;
        boolean combatFrame;
        boolean instantSnap;

        boolean isCombatLike() {
            return attacking || combatFrame || instantSnap;
        }
    }
}
