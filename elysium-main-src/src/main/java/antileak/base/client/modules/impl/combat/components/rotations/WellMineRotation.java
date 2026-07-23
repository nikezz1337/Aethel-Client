package antileak.base.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import antileak.base.api.QClient;
import antileak.base.api.storages.implement.RotationStorage;
import antileak.base.api.utils.rotate.Rotation;
import antileak.base.client.modules.impl.combat.Aura;
import antileak.base.client.modules.impl.combat.components.RotationsSystem;

public class WellMineRotation extends RotationsSystem implements QClient {
    private LivingEntity currentTarget;

    private float lastYaw = 0.0F;
    private float lastPitch = 0.0F;

    private float acceleration = 0.0F;
    private boolean isBack = false;

    private double randomOffsetX = 0.0;
    private double randomOffsetY = 0.0;
    private double randomOffsetZ = 0.0;

    public void reset() {
        currentTarget = null;
        acceleration = 0.0F;
        isBack = false;
        randomOffsetX = 0.0;
        randomOffsetY = 0.0;
        randomOffsetZ = 0.0;

        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        } else {
            lastYaw = 0.0F;
            lastPitch = 0.0F;
        }
    }

    private float getGCDValue() {
        float sensitivity = (float) (mc.options.getMouseSensitivity().getValue() * 0.6F + 0.2F);
        return sensitivity * sensitivity * sensitivity * 1.2F;
    }

    
    private void updateRandomOffset(LivingEntity target) {
        Box box = target.getBoundingBox();
        double boxWidth = box.maxX - box.minX;
        double boxHeight = box.maxY - box.minY;
        double boxDepth = box.maxZ - box.minZ;

        randomOffsetX = (Math.random() - 0.5) * boxWidth * 0.15;
        randomOffsetY = (Math.random() - 0.5) * boxHeight * 0.15;
        randomOffsetZ = (Math.random() - 0.5) * boxDepth * 0.15;
    }

    
    @Override
    public void updateRotations(LivingEntity target) {
        if (mc.player == null || target == null) {
            return;
        }

        if (currentTarget != target) {
            currentTarget = target;
            acceleration = 0.0F;
            isBack = false;
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
            updateRandomOffset(target);
        }

        Box box = getPredictedBox(target);
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d centerPoint = box.getCenter().add(randomOffsetX, randomOffsetY, randomOffsetZ);
        Vec3d toTarget = centerPoint.subtract(eyePos);
        float centerYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
        float centerPitch = (float) (-Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))));
        boolean bothGliding = mc.player.isGliding() && target.isGliding();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Vec3d endVec = eyePos.add(lookVec.multiply(bothGliding ? 1488.0 : 999.0));
        Box shrunkBox = box.expand(bothGliding ? 0.0 : -0.5);
        boolean inBox = shrunkBox.raycast(eyePos, endVec).isPresent();

        if (bothGliding) {
            if (isBack) {
                if (acceleration >= -0.02F) {
                    acceleration -= Math.abs(MathHelper.wrapDegrees(centerYaw - lastYaw)) > 80.0F ? 0.15F : 0.02F;
                }
                if (acceleration <= -0.02F) {
                    isBack = false;
                    updateRandomOffset(target);
                }
            } else {
                acceleration += 0.0105F;
                if (acceleration >= 0.305F || inBox) {
                    isBack = true;
                }
            }
        } else if (isBack) {
            if (acceleration >= -0.15F) {
                float slowdownSpeed = Math.abs(MathHelper.wrapDegrees(centerYaw - lastYaw)) > 80.0F ? 0.1F : 0.01F;
                acceleration -= (slowdownSpeed *= 0.9F + (float) Math.random() * 0.2F);
            }
            if (acceleration <= -0.15F) {
                isBack = false;
                updateRandomOffset(target);
            }
        } else {
            float accelSpeed = 0.0082F + ((float) Math.random() * 0.002F - 0.001F);
            acceleration += accelSpeed;
            float threshold = 0.184F + ((float) Math.random() * 0.03F - 0.015F);
            if (acceleration >= threshold || inBox) {
                isBack = true;
            }
        }

        float deltaYaw = MathHelper.wrapDegrees(centerYaw - lastYaw);
        float deltaPitch = centerPitch - lastPitch;
        float smooth = Math.max(acceleration, 0.0F);
        float humanYawOffset = (float) (Math.sin((double) System.currentTimeMillis() * 0.001) * 0.04);
        float humanPitchOffset = (float) (Math.cos((double) System.currentTimeMillis() * 0.0015) * 0.025);
        if (Math.abs(deltaYaw) > 1.0F || Math.abs(deltaPitch) > 1.0F) {
            humanYawOffset += ((float) Math.random() - 0.5F) * 0.035F;
            humanPitchOffset += ((float) Math.random() - 0.5F) * 0.02F;
        }

        float newYaw = lastYaw + deltaYaw * MathHelper.clamp(smooth * 1.12F, 0.0F, 1.0F) + humanYawOffset;
        float newPitch = lastPitch + deltaPitch * MathHelper.clamp(smooth / 1.88F, 0.0F, 1.0F) + humanPitchOffset;
        float gcd = getGCDValue();
        newYaw -= (newYaw - lastYaw) % gcd;
        newPitch -= (newPitch - lastPitch) % gcd;
        if (newPitch > 89.0F) {
            newPitch = 89.0F;
        }
        if (newPitch < -89.0F) {
            newPitch = -89.0F;
        }

        lastYaw = newYaw;
        lastPitch = newPitch;
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0F, 45.0F, 45.0F, 45.0F, 0, 1, Aura.clientLook.isState());
    }
}
