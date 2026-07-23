package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.entity.MovingUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.InputEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.modules.combat.Aura;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Speed extends Module {
    public static Speed getInstance() {
        return Instance.get(Speed.class);
    }

    SelectSetting mode = new SelectSetting("Mode", "Select speed mode")
            .value("Default", "Motion").selected("Motion");

    ValueSetting speed = new ValueSetting("Boost Strength", "Strength of the speed boost")
            .setValue(3f).range(1f, 5f);


    public Speed() {
        super("Speed", ModuleCategory.MOVEMENT);
        setup(speed);
    }

    @EventHandler
    public void onTick (TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (MovingUtil.hasPlayerMovement()) {
            int collisions = 0;
            Box expandedBox = mc.player.getBoundingBox().expand(0.5f);

            for (Entity ent : mc.world.getEntities()) {
                if (!(ent instanceof PlayerEntity)) continue;

                if (ent != mc.player && (ent instanceof LivingEntity || ent instanceof BoatEntity)
                        && expandedBox.intersects(ent.getBoundingBox())) {
                    collisions++;
                }
            }

            double[] motion = forward(speed.getValue() * 0.01 * collisions);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
        }
    }

    public static double[] forward(final double d) {
        float f = mc.player.input.movementForward;
        float f2 = mc.player.input.movementSideways;
        float f3 = mc.player.getYaw();
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        final double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        final double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        final double d4 = f * d * d3 + f2 * d * d2;
        final double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }
}
