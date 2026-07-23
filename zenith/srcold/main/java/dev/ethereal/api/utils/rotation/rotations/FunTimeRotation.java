package dev.ethereal.api.utils.rotation.rotations;

import dev.ethereal.api.utils.math.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationMode;

import java.util.concurrent.ThreadLocalRandom;

public class FunTimeRotation extends RotationMode {

    public FunTimeRotation() {
        super("FunTime");
    }

    private float lerpAngle(float current, float target, float t) {
        float delta = MathHelper.wrapDegrees(target - current);
        return current + delta * t;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        if (vec3d == null || entity == null) {
            return targetRotation;
        }

        float newYaw = lerpAngle(currentRotation.getYaw(), targetRotation.getYaw(), 0.4f);
        float newPitch = lerpAngle(currentRotation.getPitch(), targetRotation.getPitch(), 0.4f);

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
    }
}