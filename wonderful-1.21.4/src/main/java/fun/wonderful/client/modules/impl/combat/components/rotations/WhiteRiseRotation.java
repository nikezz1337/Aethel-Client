package fun.wonderful.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import fun.wonderful.api.QClient;
import fun.wonderful.api.storages.implement.RotationStorage;
import fun.wonderful.api.utils.rotate.Rotation;
import fun.wonderful.api.utils.rotate.RotationUtils;
import fun.wonderful.client.modules.impl.combat.Aura;
import fun.wonderful.client.modules.impl.combat.components.RotationsSystem;
import fun.wonderful.client.modules.impl.combat.components.gcd.GCDUtil;
import fun.wonderful.client.modules.impl.combat.components.interpolation.BestPoint;

import java.util.concurrent.ThreadLocalRandom;

public class WhiteRiseRotation extends RotationsSystem implements QClient {

    private final Aura aura;
    private LivingEntity trackedTarget;
    private float lastYaw;
    private float lastPitch;
    private float speedAcceleration;
    private boolean back;
    private boolean initialized;
    private float jitterOffset;
    private int tickCounter;

    public WhiteRiseRotation(Aura aura) {
        this.aura = aura;
    }

    public void reset() {
        trackedTarget = null;
        speedAcceleration = 0.00F;
        back = false;
        jitterOffset = 0.0F;
        tickCounter = 0;
        initialized = mc.player != null;

        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        } else {
            lastYaw = 0.0F;
            lastPitch = 0.0F;
        }
    }

    public void onAttack() {
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

        if (!initialized) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
            initialized = true;
        }

        if (trackedTarget != target) {
            trackedTarget = target;
            speedAcceleration = 0.0F;
            back = false;
            tickCounter = 0;
        }

        tickCounter++;
        jitterOffset = (float) (((Math.sin(tickCounter * 0.17) * 0.12) + (Math.random() * 0.08 - 0.04)) * 0.7F);

        Vec3d point = BestPoint.getMultipoint(target, 128.0);
        Vec2f angle = RotationUtils.getRotations(point);
        float targetYaw = angle.x;
        float targetPitch = angle.y;

        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - lastYaw));
        boolean readyToAttack = mc.player.getAttackCooldownProgress(1.0F) > 0.9F && aura.getWhiteRiseTicksToAttack() <= 1;

        if (!back) {
            float gain = 0.0055F;
            if (yawDiff > 60.0F) {
                gain += 0.016F * 1.8F;
            } else if (yawDiff > 30.0F) {
                gain += 0.008F * 1.8F;
            } else {
                gain += 0.004F * 1.8F;
            }
            if (readyToAttack) {
                gain += 0.018F / 1.4F;
            }
            speedAcceleration += gain * (1.6F + jitterOffset);
            if (speedAcceleration >= 0.22F) back = true;
        } else {
            float loss = readyToAttack ? 0.045F : 0.008F;
            speedAcceleration -= loss * (2.1F + jitterOffset);
            if (speedAcceleration <= -0.04F) back = false;
        }

        float smooth = MathHelper.clamp(speedAcceleration, 0.0F, mc.player.isGliding() ? 0.38F : 0.26F);
        if (readyToAttack) {
            smooth = Math.min(smooth + 0.1F, mc.player.isGliding() ? 0.46F : 0.34F);
        }
        smooth += jitterOffset * 0.5F;
        if (tickCounter % 7 == 0) smooth += 0.03F;

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float deltaPitch = targetPitch - lastPitch;

        float yawLimit = mc.player.isGliding() ? 42.0F : (readyToAttack ? 28.0F : 20.0F);
        float pitchLimit = mc.player.isGliding() ? 12.0F : (readyToAttack ? 4.5F : 2.8F);

        deltaYaw = MathHelper.clamp(deltaYaw, -yawLimit, yawLimit);
        deltaPitch = MathHelper.clamp(deltaPitch, -pitchLimit, pitchLimit);

        float pitchSpeed = smooth * 0.28F;
        float yawSpeed = smooth * (0.85F + (jitterOffset * 0.4F));

        float newYaw = lastYaw + deltaYaw * yawSpeed;
        float newPitch = lastPitch + deltaPitch * pitchSpeed;

        float gcd = GCDUtil.getGCDValue();
        if (gcd > 0.0F) {
            newYaw = lastYaw + Math.round((newYaw - lastYaw) / gcd) * gcd;
            newPitch = lastPitch + Math.round((newPitch - lastPitch) / gcd) * gcd;
        }

        newPitch = MathHelper.clamp(newPitch, -89.0F, 89.0F);

        Rotation finalRot = new Rotation(newYaw, newPitch);
        float rotSpeed = mc.player.isGliding() && target.isGliding() ? 360.0F : 45.0F;
        RotationStorage.update(finalRot, rotSpeed, rotSpeed, rotSpeed, rotSpeed, 0, 1, Aura.clientLook.isState());

        rotate = new Vec2f(finalRot.getYaw(), finalRot.getPitch());
        lastYaw = finalRot.getYaw();
        lastPitch = finalRot.getPitch();
    }

    private Vec3d getAimPoint(LivingEntity target) {
        Vec3d point = BestPoint.getPoint(target);
        if (point == null) {
            point = target.getBoundingBox().getCenter();
        }
        if (shouldUseElytraPredict(target)) {
            return getPredictedPoint(target, point);
        }
        return point;
    }
}