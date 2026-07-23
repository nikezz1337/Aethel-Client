package antileak.base.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
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
import antileak.base.client.modules.impl.combat.components.interpolation.BestPoint;

import java.util.concurrent.ThreadLocalRandom;

public class WhiteRiseRotation extends RotationsSystem implements QClient {

    private final Aura aura;
    private LivingEntity trackedTarget;
    private float lastYaw;
    private float lastPitch;
    private float velYaw;
    private float velPitch;
    private float jerkAccum;
    private float correctionDecay;
    private int tickCounter;
    private int humanDelay;
    private float dynamicScale;
    private float pointDriftX;
    private float pointDriftZ;

    public WhiteRiseRotation(Aura aura) {
        this.aura = aura;
    }

    public void reset() {
        trackedTarget = null;
        velYaw = 0.0F;
        velPitch = 0.0F;
        jerkAccum = 0.0F;
        correctionDecay = 0.0F;
        tickCounter = 0;
        humanDelay = ThreadLocalRandom.current().nextInt(2, 5);
        dynamicScale = 0.65F + ThreadLocalRandom.current().nextFloat() * 0.2F;
        pointDriftX = 0.0F;
        pointDriftZ = 0.0F;
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        } else {
            lastYaw = 0.0F;
            lastPitch = 0.0F;
        }
    }

    public void onAttack() {
        humanDelay = ThreadLocalRandom.current().nextInt(1, 4);
        jerkAccum = 0.0F;
        correctionDecay = 0.0F;
    }

    @Override
    public void updateRotations(LivingEntity target) {
        if (mc.player == null || target == null) return;

        if (mc.player.isBlocking()) {
            rotate = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
            lastYaw = rotate.x;
            lastPitch = rotate.y;
            return;
        }

        if (trackedTarget != target) {
            trackedTarget = target;
            velYaw = 0.0F;
            velPitch = 0.0F;
            jerkAccum = 0.0F;
            tickCounter = 0;
            humanDelay = ThreadLocalRandom.current().nextInt(2, 5);
            dynamicScale = 0.65F + ThreadLocalRandom.current().nextFloat() * 0.2F;
        }

        tickCounter++;
        if (humanDelay > 0) {
            humanDelay--;
            rotate = new Vec2f(lastYaw, lastPitch);
            return;
        }

        pointDriftX += (float) (Math.sin(tickCounter * 0.08) * 0.003);
        pointDriftZ += (float) (Math.cos(tickCounter * 0.11) * 0.004);
        pointDriftX = MathHelper.clamp(pointDriftX, -0.12F, 0.12F);
        pointDriftZ = MathHelper.clamp(pointDriftZ, -0.12F, 0.12F);

        Vec3d basePoint = BestPoint.getMultipoint(target, 256);
        Vec3d driftPoint = basePoint.add(pointDriftX, 0.0, pointDriftZ);
        Vec2f angle = RotationUtils.getRotations(driftPoint);
        float targetYaw = angle.x;
        float targetPitch = angle.y;

        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - lastYaw));
        boolean readyToAttack = mc.player.getAttackCooldownProgress(1.0F) > 0.9F && aura.getWhiteRiseTicksToAttack() <= 1;

        float baseAccel = 0.0028F;
        if (yawDiff > 80.0F) baseAccel += 0.015F;
        else if (yawDiff > 45.0F) baseAccel += 0.0085F;
        else if (yawDiff > 20.0F) baseAccel += 0.0035F;
        else baseAccel += 0.0009F;

        if (readyToAttack) baseAccel += 0.0045F;

        float noise = (float) Math.sin(tickCounter * 0.13 + ThreadLocalRandom.current().nextFloat() * 0.5) * 0.025F;
        float accelMod = dynamicScale * (0.92F + noise);

        if (!readyToAttack) {
            velYaw += baseAccel * accelMod;
            if (yawDiff < 12.0F) velYaw *= 0.85F;
        } else {
            velYaw += baseAccel * accelMod * 1.15F;
        }

        velYaw = MathHelper.clamp(velYaw, 0.0F, readyToAttack ? 0.18F : 0.11F);

        if (tickCounter % ThreadLocalRandom.current().nextInt(7, 12) == 0) {
            jerkAccum = (float) (ThreadLocalRandom.current().nextDouble() * 2.2 - 1.1);
            correctionDecay = 0.72F + ThreadLocalRandom.current().nextFloat() * 0.15F;
        } else {
            jerkAccum *= correctionDecay;
        }

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float deltaPitch = targetPitch - lastPitch;

        float pitchThreshold = 1.8F;
        if (Math.abs(deltaPitch) > pitchThreshold) {
            float pitchAccel = baseAccel * 0.14F * accelMod;
            velPitch += pitchAccel * (readyToAttack ? 0.65F : 0.45F);
            velPitch = MathHelper.clamp(velPitch, 0.0F, readyToAttack ? 0.038F : 0.022F);
        } else {
            velPitch *= 0.78F;
        }

        float maxYawDelta = readyToAttack ? 26.0F : 16.0F;
        float maxPitchDelta = readyToAttack ? 3.2F : 1.6F;

        deltaYaw = MathHelper.clamp(deltaYaw, -maxYawDelta, maxYawDelta);
        deltaPitch = MathHelper.clamp(deltaPitch, -maxPitchDelta, maxPitchDelta);

        float yawMove = deltaYaw * velYaw + jerkAccum;
        float pitchMove = deltaPitch * velPitch;

        if (Math.abs(jerkAccum) > 0.4F) pitchMove *= 0.65F;

        float newYaw = lastYaw + yawMove;
        float newPitch = lastPitch + pitchMove;

        float gcd = GCDUtil.getGCDValue();
        if (gcd > 0.0F && ThreadLocalRandom.current().nextFloat() > 0.22F) {
            float yawSnap = Math.round((newYaw - lastYaw) / gcd) * gcd;
            float pitchSnap = Math.round((newPitch - lastPitch) / gcd) * gcd;
            float deviation = gcd * (ThreadLocalRandom.current().nextFloat() * 0.35F - 0.175F);
            newYaw = lastYaw + yawSnap + deviation;
            newPitch = lastPitch + pitchSnap + deviation * 0.3F;
        } else if (gcd > 0.0F) {
            newYaw += ThreadLocalRandom.current().nextFloat() * 0.08F - 0.04F;
            newPitch += ThreadLocalRandom.current().nextFloat() * 0.04F - 0.02F;
        }

        newPitch = MathHelper.clamp(newPitch, -89.0F, 89.0F);

        Rotation finalRot = new Rotation(newYaw, newPitch);
        float rotSpeed = mc.player.isGliding() && target.isGliding() ? 360.0F : 38.0F + ThreadLocalRandom.current().nextFloat() * 6.0F;
        RotationStorage.update(finalRot, rotSpeed, rotSpeed, rotSpeed, rotSpeed, 0, 1, Aura.clientLook.isState());

        rotate = new Vec2f(finalRot.getYaw(), finalRot.getPitch());
        lastYaw = finalRot.getYaw();
        lastPitch = finalRot.getPitch();
    }
}