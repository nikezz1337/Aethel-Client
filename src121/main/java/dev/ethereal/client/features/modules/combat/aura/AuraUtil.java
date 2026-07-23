package dev.ethereal.client.features.modules.combat.aura;

import dev.ethereal.api.system.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AuraUtil implements QuickImports {

    public static double getStrictDistance(LivingEntity target) {
        if (mc.player == null || target == null) return Double.MAX_VALUE;
        return mc.player.distanceTo(target);
    }

    public static float calculateFOVFromCamera(LivingEntity target) {
        if (mc.player == null || target == null) return 0;
        Vec3d targetVec = target.getPos().add(0, target.getHeight() / 2.0F, 0)
                .subtract(mc.player.getEyePos()).normalize();
        Vec3d lookVec = mc.player.getRotationVecClient();
        return (float) Math.toDegrees(Math.acos(MathHelper.clamp(
                lookVec.dotProduct(targetVec), -1, 1
        )));
    }

    public static Vec3d getSpookyVector(LivingEntity target) {
        if (mc.player == null || target == null) return Vec3d.ZERO;
        return target.getPos()
                .add(0, MathHelper.clamp(mc.player.getEyePos().y - target.getY(), 0.0F, 0.5F), 0)
                .subtract(mc.player.getEyePos()).normalize();
    }

    public static Vec3d getCenter(LivingEntity entity) {
        return entity.getPos().add(0, entity.getHeight() / 2.0F, 0);
    }
}
