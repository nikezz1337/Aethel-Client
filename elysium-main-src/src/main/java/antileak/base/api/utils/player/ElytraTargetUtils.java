package antileak.base.api.utils.player;

import net.minecraft.entity.LivingEntity;
import antileak.base.api.QClient;
import antileak.base.client.modules.impl.combat.ElytraMotion;
import antileak.base.client.modules.impl.combat.ElytraTarget;

public class ElytraTargetUtils implements QClient {
    public static void updateNumber() {
    }

    public static float getJitterYaw(LivingEntity entity) {
        return 0;
    }

    public static float getJitterPitch(LivingEntity entity) {
        return 0;
    }

    public static boolean canTarget(LivingEntity target) {
        if (target == null) return false;
        if (mc.player.getCooledAttackStrength(1.5f) < 0.94f) return true;
        if (mc.player.isHandActive()) return true;

        return false;
    }

    public static boolean fullCheck() {
        if (mc.player == null || mc.world == null) return false;
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        ElytraMotion elytraMotion = ElytraMotion.INSTANCE;

        return elytraTarget != null
                && elytraTarget.isEnable()
                && mc.player.isGliding()
                && (elytraMotion == null || !elytraMotion.isEnable());
    }
}
