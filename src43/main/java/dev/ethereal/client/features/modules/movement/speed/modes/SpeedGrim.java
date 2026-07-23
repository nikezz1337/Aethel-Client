package dev.ethereal.client.features.modules.movement.speed.modes;

import dev.ethereal.api.utils.player.PredictUtils;
import dev.ethereal.client.features.modules.combat.AuraModule;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.system.client.TimerManager;
import dev.ethereal.api.utils.task.TaskPriority;
import dev.ethereal.client.features.modules.movement.speed.SpeedMode;
import dev.ethereal.client.features.modules.movement.speed.SpeedModule;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class SpeedGrim extends SpeedMode {
    @Override
    public String getName() {
        return "Grim";
    }

    public BypassType bypassType;
    private boolean boosting;
    private final TimerUtil timerUtil = new TimerUtil();

    @Getter private final ModeSetting grimType = new ModeSetting("Grim mode").value(BypassType.COLLIDE)
            .values(BypassType.values())
            .onAction(() -> {
                bypassType = switch (getGrimType().getValue()) {
                    case "Timer" -> SpeedGrim.BypassType.TIMER;
                    case "Collide new" -> SpeedGrim.BypassType.COLLIDE_NEW;
                    default -> SpeedGrim.BypassType.COLLIDE;
                };
            });

    public SpeedGrim(Supplier<Boolean> condition) {
        grimType.setVisible(condition);
        addSettings(grimType);
    }

    @Override
    public void onTravel() {
        switch (bypassType) {
            case COLLIDE, COLLIDE_NEW -> {
                boolean newMode = bypassType == BypassType.COLLIDE_NEW;
                int collisions = 0;
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof LivingEntity living) {
                        if (living == mc.player) continue;
                        if (living instanceof ArmorStandEntity) continue;
                        if (PlayerUtil.hasCollisionWith(living, newMode ? 0.5f : 1f)) {
                            collisions++;
                        }
                    }
                }

                if (collisions > 0 && !mc.player.isOnGround()) {
                    double[] forward = MoveUtil.forward(0.08 * collisions);
                    mc.player.addVelocity(forward[0], 0.0, forward[1]);
                }
            }


            default -> {
                if (timerUtil.finished(1100)) {
                    boosting = true;
                }

                if (timerUtil.finished(7000)) {
                    boosting = false;
                    timerUtil.reset();
                }

                TimerManager.getInstance().addTimer(boosting ? mc.player.age % 2 == 0 ? 1.5f : 1.2f : 0.05f, TaskPriority.HIGH, SpeedModule.getInstance(), 1);
            }
        }
    }

    private Vec3d calculateVelocity(LivingEntity target) {
        double deltaX;
        double deltaZ;

        Vec3d predictedPos = PredictUtils.predict(target, 1.5F);
        deltaX = predictedPos.x - mc.player.getX();
        deltaZ = predictedPos.z - mc.player.getZ();

        float targetYaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
        double radYaw = Math.toRadians(targetYaw);

        double force = 0.072D;

        Vec3d currentVelocity = mc.player.getVelocity();

        return new Vec3d(
                currentVelocity.x + -Math.sin(radYaw) * force,
                currentVelocity.y,
                currentVelocity.z + Math.cos(radYaw) * force
        );
    }

    public enum BypassType implements ModeSetting.NamedChoice {
        COLLIDE("Collide"),
        COLLIDE_NEW("Collide new"),
        TIMER("Timer");

        private final String name;

        BypassType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
