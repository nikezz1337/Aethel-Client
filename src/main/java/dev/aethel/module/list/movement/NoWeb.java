package dev.aethel.module.list.movement;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(
        moduleName = "NoWeb",
        moduleCategory = ModuleCategory.MOVEMENT,
        moduleDesc = "Убирает замедление от паутины"
)
public class NoWeb extends Module {

    private final SliderSetting speed =
            new SliderSetting("Скорость", 0.66, 0.1, 1.0, 0.01);

    public NoWeb() {
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (!isInWeb()) return;

        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;
        if (forward == 0 && strafe == 0) return;

        float yaw = mc.player.getYaw() * 0.017453292F;

        double x = (-Math.sin(yaw) * forward) + (Math.cos(yaw) * strafe);
        double z = (Math.cos(yaw) * forward) + (Math.sin(yaw) * strafe);

        float spd = speed.getFloatValue();
        x *= 0.23 * spd;
        z *= 0.23 * spd;

        double y = mc.player.getVelocity().y;

        if (mc.options.jumpKey.isPressed()) {
            y += 0.04 * spd;
        }
        if (mc.options.sneakKey.isPressed()) {
            y -= 0.04 * spd;
        }

        double currentSpeed = Math.sqrt(x * x + z * z);
        double maxSpeed = 0.53 * spd;
        if (currentSpeed > maxSpeed) {
            double scale = maxSpeed / currentSpeed;
            x *= scale;
            z *= scale;
        }

        mc.player.setVelocity(x, y, z);

        if (mc.player.horizontalCollision || mc.player.verticalCollision) {
            Vec3d vel = mc.player.getVelocity();
            mc.player.setVelocity(vel.x, vel.y, vel.z);
        }
    }

    private boolean isInWeb() {
        BlockPos pos = mc.player.getBlockPos();
        var state = mc.world.getBlockState(pos);
        var stateUp = mc.world.getBlockState(pos.up());
        return state.isOf(Blocks.COBWEB) || stateUp.isOf(Blocks.COBWEB) ||
               state.isOf(Blocks.SWEET_BERRY_BUSH) || stateUp.isOf(Blocks.SWEET_BERRY_BUSH);
    }
}
