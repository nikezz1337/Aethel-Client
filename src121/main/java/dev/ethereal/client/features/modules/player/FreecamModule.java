package dev.ethereal.client.features.modules.player;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.player.DirectionalInput;

@ModuleRegister(name = "Freecam", category = Category.PLAYER)
public class FreecamModule extends Module {
    @Getter private static final FreecamModule instance = new FreecamModule();

    private final SliderSetting speed = new SliderSetting("Speed").value(1.0f).range(0.1f, 5.0f).step(0.1f);
    private final BooleanSetting verticalSpeed = new BooleanSetting("Vertical Speed").value(true);
    private final SliderSetting vSpeed = new SliderSetting("V Speed").value(1.0f).range(0.1f, 5.0f).step(0.1f).setVisible(verticalSpeed::getValue);

    private Vec3d pos = Vec3d.ZERO;
    private Vec3d prevPos = Vec3d.ZERO;
    @Getter private float yaw;
    @Getter private float pitch;

    public FreecamModule() {
        addSettings(speed, verticalSpeed, vSpeed);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }
        pos = mc.player.getEyePos();
        prevPos = pos;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
    }

    public Vec3d getRenderPos(float tickDelta) {
        return prevPos.add(pos.subtract(prevPos).multiply(tickDelta));
    }

    public void applyMouseInput(double cursorDeltaX, double cursorDeltaY) {
        yaw += (float) cursorDeltaX * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) cursorDeltaY * 0.15f, -90f, 90f);
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        event.setDirectionalInput(DirectionalInput.NONE);
        event.setJump(false);
        event.setSneak(false);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        prevPos = pos;
        double rad = Math.toRadians(yaw);
        double x = 0, z = 0;
        if (mc.options.forwardKey.isPressed()) { x -= Math.sin(rad); z += Math.cos(rad); }
        if (mc.options.backKey.isPressed()) { x += Math.sin(rad); z -= Math.cos(rad); }
        if (mc.options.leftKey.isPressed()) { x += Math.cos(rad); z += Math.sin(rad); }
        if (mc.options.rightKey.isPressed()) { x -= Math.cos(rad); z -= Math.sin(rad); }
        Vec3d horizontal = new Vec3d(x, 0, z);
        if (horizontal.lengthSquared() > 0) {
            horizontal = horizontal.normalize().multiply(speed.getValue());
        }
        double y = 0;
        double vertical = verticalSpeed.getValue() ? vSpeed.getValue() : speed.getValue();
        if (mc.options.jumpKey.isPressed()) y += vertical;
        if (mc.options.sneakKey.isPressed()) y -= vertical;
        pos = pos.add(horizontal.x, y, horizontal.z);
    }
}
