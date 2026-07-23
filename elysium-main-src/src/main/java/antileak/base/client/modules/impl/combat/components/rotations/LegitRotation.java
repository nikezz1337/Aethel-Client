package antileak.base.client.modules.impl.combat.components.rotations;

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

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class LegitRotation extends RotationsSystem implements QClient {

    @Override
    public void updateRotations(LivingEntity target) {
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Vec3d reachVec = eyePos.add(lookVec.multiply(999));

        Box box = getPredictedBox(target);

        double shrinkXZ = target.isGliding() ? -0.5f : 0.1f;
        double shrinkY = target.isGliding() ? -0.5f : 0.1f;

        box = new Box(
                box.minX + (box.getLengthX() * shrinkXZ / 2),
                box.minY,
                box.minZ + (box.getLengthZ() * shrinkXZ / 2),
                box.maxX - (box.getLengthX() * shrinkXZ / 2),
                box.maxY - (box.getLengthY() * shrinkY),
                box.maxZ - (box.getLengthZ() * shrinkXZ / 2)
        );

        Optional<Vec3d> hit = box.raycast(eyePos, reachVec);
        boolean inside = box.contains(eyePos);

        if (hit.isPresent() || inside) {
            Aura.adjYaw = MathHelper.clamp(Aura.adjYaw - ThreadLocalRandom.current().nextFloat(0.005f, 0.02f), 0, 1);
            Aura.adjPitch = MathHelper.clamp(Aura.adjPitch - ThreadLocalRandom.current().nextFloat(0.005f, 0.02f), 0, 1);
        } else {
            if (mc.player.isGliding()) {
                Aura.adjYaw = MathHelper.clamp(Aura.adjYaw + ThreadLocalRandom.current().nextFloat(0.0005f, 0.005f), 0, 1);
                Aura.adjPitch = MathHelper.clamp(Aura.adjPitch + ThreadLocalRandom.current().nextFloat(0.0009f, 0.009f), 0, 1);
            } else {
                if (target.isInSwimmingPose()) {
                    Aura.adjYaw = MathHelper.clamp(Aura.adjYaw + ThreadLocalRandom.current().nextFloat(0.00009f, 0.009f), 0, 1);
                    Aura.adjPitch = MathHelper.clamp(Aura.adjPitch + ThreadLocalRandom.current().nextFloat(0.00009f, 0.0009f), 0, 1);
                } else {
                    Aura.adjYaw = MathHelper.clamp(Aura.adjYaw + ThreadLocalRandom.current().nextFloat(0.00009f, 0.009f), 0, 1);
                    Aura.adjPitch = MathHelper.clamp(Aura.adjPitch + ThreadLocalRandom.current().nextFloat(0.0009f, 0.009f), 0, 1);
                }
            }
        }

        Vec2f targetRot;
        targetRot = RotationUtils.getRotations(getPredictedPoint(target, target.getLeashPos(1)));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float diffYaw = MathHelper.wrapDegrees(targetRot.x - currentYaw);
        float diffPitch = MathHelper.wrapDegrees(targetRot.y - currentPitch);

        float newYaw = currentYaw + diffYaw * Aura.adjYaw;
        float newPitch = currentPitch + diffPitch * Aura.adjPitch;

        Aura.otvodkaYaw = 0;
        Aura.otvodkaPitch = 0;
        RotationStorage.update(
                new Rotation(newYaw, newPitch),
                360, 360,
                40, 35, 1,
                1, Aura.clientLook.isState()
        );
    }
}
