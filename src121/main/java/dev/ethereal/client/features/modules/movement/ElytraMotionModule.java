package dev.ethereal.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.move.MoveEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.client.features.modules.combat.Aura;
import dev.ethereal.client.features.modules.combat.elytratarget.ElytraTargetModule;
import dev.ethereal.client.features.modules.player.ElytraSwapModule;

@ModuleRegister(name = "Elytra Motion", category = Category.MOVEMENT)
public class ElytraMotionModule extends Module {
    @Getter private static final ElytraMotionModule instance = new ElytraMotionModule();

    private final SliderSetting distance = new SliderSetting("Distance").value(3f).range(0.1f, 6f).step(0.1f);

    public ElytraMotionModule() {
        addSettings(distance);
    }

    @EventHandler(priority = 0)
    public void onMove(MoveEvent event) {
        Aura aura = Aura.getInstance();
        LivingEntity target = aura.getTarget();

        if (!aura.isEnabled() || target == null) return;

        Vec3d targetPos = RotationUtil.getSpot(target);
        ElytraTargetModule elytraTarget = ElytraTargetModule.getInstance();

        if (elytraTarget.isEnabled() && elytraTarget.elytraRotationProcessor.using()) {
            targetPos = elytraTarget.elytraRotationProcessor.getPredictedPos(target);
        }

        float targetDistance = (float) targetPos.distanceTo(mc.player.getEyePos());

        if (targetDistance < distance.getValue() && mc.player.isGliding()) {
            event.set(Vec3d.ZERO);
            event.setCancel(true);
        }
    }
}
