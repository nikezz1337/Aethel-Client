package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventHUD;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(
        moduleName = "Free Camera",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Свободная камера"
)
public class FreeCamera extends Module {

    public final SliderSetting speedSetting = new SliderSetting("Скорость", 1.0, 0.1, 10.0, 0.1);
    public final BooleanSetting airStuck = new BooleanSetting("Воздушный стоп", true);
    public final BooleanSetting positionDelta = new BooleanSetting("Позиция", true);

    private Vec3d cameraPos;
    private float prevYaw;
    private float prevPitch;
    private int tickCounter;

    public Vec3d getCameraPos() {
        return cameraPos;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null && mc.world != null) {
            cameraPos = mc.player.getEyePos();
            prevYaw = mc.player.getYaw();
            prevPitch = mc.player.getPitch();
            tickCounter = 0;
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cameraPos = null;
        tickCounter = 0;
        if (mc.player != null) {
            mc.player.setYaw(prevYaw);
            mc.player.setPitch(prevPitch);
        }
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || cameraPos == null) return;
        mc.player.setInvisible(true);

        GameOptions opts = mc.options;
        float speed = (float) speedSetting.getValue();
        float forward = 0, sideways = 0, vertical = 0;

        if (opts.forwardKey.isPressed()) forward++;
        if (opts.backKey.isPressed()) forward--;
        if (opts.leftKey.isPressed()) sideways++;
        if (opts.rightKey.isPressed()) sideways--;
        if (opts.jumpKey.isPressed()) vertical++;
        if (opts.sneakKey.isPressed()) vertical--;

        if (forward != 0 || sideways != 0 || vertical != 0) {
            float yawRad = mc.player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            Vec3d move = Vec3d.ZERO;
            if (forward != 0) move = move.add(-MathHelper.sin(yawRad) * forward, 0, MathHelper.cos(yawRad) * forward);
            if (sideways != 0) move = move.add(MathHelper.sin(yawRad - 1.5708f) * sideways, 0, -MathHelper.cos(yawRad - 1.5708f) * sideways);
            if (vertical != 0) move = move.add(0, vertical, 0);
            cameraPos = cameraPos.add(move.normalize().multiply(speed * 0.2));
        }

        if (airStuck.getValue()) {
            mc.player.setVelocity(0, 0, 0);
            if (tickCounter % 600 < 598) {
                mc.player.setPosition(mc.player.getX() + 0.0001, mc.player.getY(), mc.player.getZ());
            }
            tickCounter++;
        }

        // sync player head rotation with camera look direction
        mc.player.setYaw(mc.player.getYaw());
        mc.player.setPitch(mc.player.getPitch());
    }

    @Subscribe
    public void onHUD(EventHUD event) {
        if (!positionDelta.getValue() || cameraPos == null || mc.player == null) return;

        DrawContext ctx = event.getDrawContext();
        Vec3d delta = cameraPos.subtract(mc.player.getEyePos());
        String info = String.format("X %.1f | Y %.1f | Z %.1f", delta.x, delta.y, delta.z);
        String dist = String.format("hypot: %.1f", delta.length());
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        ctx.drawTextWithShadow(mc.textRenderer, info, w / 2 - mc.textRenderer.getWidth(info) / 2,
                h / 2 + 40, 0xFF888888);
        ctx.drawTextWithShadow(mc.textRenderer, dist, w / 2 - mc.textRenderer.getWidth(dist) / 2,
                h / 2 + 52, 0xFF888888);
    }
}
