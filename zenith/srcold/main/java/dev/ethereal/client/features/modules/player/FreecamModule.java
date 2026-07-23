package dev.ethereal.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;

@ModuleRegister(name = "Freecam", category = Category.PLAYER)
public class FreecamModule extends Module {
    @Getter private static final FreecamModule instance = new FreecamModule();

    private final SliderSetting speed = new SliderSetting("Speed").value(1.0f).range(0.1f, 5.0f).step(0.1f);
    private final BooleanSetting verticalSpeed = new BooleanSetting("Vertical Speed").value(true);
    private final SliderSetting vSpeed = new SliderSetting("V Speed").value(1.0f).range(0.1f, 5.0f).step(0.1f).setVisible(verticalSpeed::getValue);

    private Vec3d originalPos;
    private float originalYaw;
    private float originalPitch;
    private Perspective originalPerspective;

    public FreecamModule() {
        addSettings(speed, verticalSpeed, vSpeed);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }

        originalPos = mc.player.getPos();
        originalYaw = mc.player.getYaw();
        originalPitch = mc.player.getPitch();
        originalPerspective = mc.options.getPerspective();
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        mc.player.setPosition(originalPos);
        mc.player.setYaw(originalYaw);
        mc.player.setPitch(originalPitch);
        
        if (originalPerspective != null) {
            mc.options.setPerspective(originalPerspective);
        }
    }

    @Override
    public void onEvent() {
        EventListener motionEvent = MotionEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            // Вычисляем направление движения
            Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw()).normalize();
            Vec3d right = Vec3d.fromPolar(0, mc.player.getYaw() + 90).normalize();

            double moveX = 0;
            double moveY = 0;
            double moveZ = 0;

            float currentSpeed = speed.getValue();

            // Горизонтальное движение
            if (mc.options.forwardKey.isPressed()) {
                moveX += forward.x * currentSpeed;
                moveZ += forward.z * currentSpeed;
            }
            if (mc.options.backKey.isPressed()) {
                moveX -= forward.x * currentSpeed;
                moveZ -= forward.z * currentSpeed;
            }
            if (mc.options.leftKey.isPressed()) {
                moveX -= right.x * currentSpeed;
                moveZ -= right.z * currentSpeed;
            }
            if (mc.options.rightKey.isPressed()) {
                moveX += right.x * currentSpeed;
                moveZ += right.z * currentSpeed;
            }

            // Вертикальное движение
            if (verticalSpeed.getValue()) {
                float vSpeedValue = vSpeed.getValue();
                if (mc.options.jumpKey.isPressed()) {
                    moveY += vSpeedValue;
                }
                if (mc.options.sneakKey.isPressed()) {
                    moveY -= vSpeedValue;
                }
            }

            // Применяем движение через fluent API
            event.x(event.x() + moveX);
            event.y(event.y() + moveY);
            event.z(event.z() + moveZ);

            // Отключаем физику
            mc.player.setVelocity(Vec3d.ZERO);
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            // Отключаем коллизии
            mc.player.noClip = true;
            mc.player.setOnGround(false);
        }));

        addEvents(motionEvent, tickEvent);
    }
}
