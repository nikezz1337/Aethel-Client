package dev.ethereal.client.features.modules.movement.fly.modes;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.system.client.TimerManager;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.rotation.RotationChanger;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import dev.ethereal.api.utils.rotation.manager.RotationPlan;
import dev.ethereal.client.features.modules.movement.fly.FlightMode;
import dev.ethereal.client.features.modules.movement.fly.FlightModule;

import java.util.function.Supplier;

public class FlightGrim extends FlightMode {
    @Override
    public String getName() {
        return "Grim";
    }

    public BypassType bypassType;
    private final FlightModule module;

    private TimerUtil ticks = new TimerUtil();
    private long speedRampStartTime = 0;
    private boolean isSpeedRamping = false;

    @Getter private final ModeSetting grimType = new ModeSetting("Режим Grim").value(BypassType.VERTICAL_ELYTRA)
            .values(BypassType.values())
            .onAction(() -> {
                bypassType = switch (getGrimType().getValue()) {
                    case "Glide elytra" -> BypassType.GLIDE_ELYTRA;
                    default -> BypassType.VERTICAL_ELYTRA;
                };
            });

    public FlightGrim(Supplier<Boolean> condition, FlightModule module) {
        grimType.setVisible(condition);
        this.module = module;
        addSettings(grimType);
    }

    @Override
    public void onUpdate() {
        if (bypassType == BypassType.GLIDE_ELYTRA) return;
        if (mc.player.isGliding() && (mc.player.getVelocity().y > 0.08 || mc.player.fallDistance > 0.1f) && (mc.player.getVelocity().x <= 0.01 && mc.player.getVelocity().z <= 0.01)) {
            mc.player.getVelocity().z = 0.0;
            mc.player.getVelocity().x = 0.0;

            RotationManager rotationManager = RotationManager.getInstance();
            Rotation rotation = rotationManager.getRotation();
            RotationPlan configurable = rotationManager.getCurrentRotationPlan();
            float pitch = configurable != null ? rotation.getPitch() : mc.player.getPitch();

            boolean validPitch = mc.player.getPitch() >= -30.0f && mc.player.getPitch() <= 30.0f;

            if (!isSpeedRamping) {
                speedRampStartTime = System.currentTimeMillis();
                isSpeedRamping = true;
            }

            long rampDuration = 100L;
            long elapsed = System.currentTimeMillis() - speedRampStartTime;
            float progress = Math.min(elapsed / (float)rampDuration, 1f);
            double currentBaseSpeed = (0.05 * progress);

            double maxAddedSpeed = 0.06;
            double maxVerticalSpeed = 1.11;

            float normalizedPitch = pitch / 90f;
            double speedAddition = maxAddedSpeed * normalizedPitch * normalizedPitch;

            double superKuniMan = currentBaseSpeed + speedAddition;
            mc.player.getVelocity().y += superKuniMan;

            if (mc.player.getVelocity().y >= maxVerticalSpeed) {
                mc.player.getVelocity().y = maxVerticalSpeed;
            }

            if (!validPitch) {
                int[] ticks = {1};
                RotationManager.getInstance().addRotation(new RotationChanger(
                        10,
                        () -> new Float[]{mc.player.getYaw(), 0f},
                        () -> --ticks[0] <= 0
                ));
            }
        } else {
            isSpeedRamping = false;
        }
    }


    /**
     * Что бы я не сделала, что бы не увидела
     * Всё, к чему коснусь — становится невидимым
     * Всё, на что смотрю — сразу испаряется
     * Дружба и любовь — со мною не случаются
     */
    @Override
    public void onMotion(MotionEvent event) {
        if (bypassType == BypassType.VERTICAL_ELYTRA || !mc.player.isGliding()) return;
        Vec3d pos = mc.player.getPos();

        float yaw = mc.player.getYaw();
        double forward = 6.087;
        double motion = MathUtil.getEntityBPS(mc.player);

        float doni = mc.getNetworkHandler().getServerInfo() != null &&  mc.getNetworkHandler().getServerInfo().address.contains("reallyworld") ? 48f : 52f;
        if (motion >= doni) {
            forward = 0f;
            motion = 0;
        }

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;
        mc.player.setVelocity(dx * MathUtil.random(1.1f, 1.21f), mc.player.getVelocity().y - 0.02f, dz * MathUtil.random(1.1f, 1.21f));

        if (ticks.finished(50)) {
            mc.player.setPosition(pos.x + dx, pos.y, pos.z + dz);
            ticks.reset();
        }
        mc.player.setVelocity(dx * MathUtil.random(1.1f, 1.21f), mc.player.getVelocity().y + 0.016f, dz * MathUtil.random(1.1f, 1.21f));
    }

    public enum BypassType implements ModeSetting.NamedChoice {
        VERTICAL_ELYTRA("Vertical elytra"),
        GLIDE_ELYTRA("Glide elytra");

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
