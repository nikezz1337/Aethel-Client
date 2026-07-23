package antileak.base.client.modules.impl.movement;

import net.minecraft.util.math.Vec3d;

import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventTickPre;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.ModeSetting;

import java.util.concurrent.ThreadLocalRandom;

public class GrimGlide extends Module {

    public static GrimGlide INSTANCE = new GrimGlide();

    private final ModeSetting mode = new ModeSetting("Режим", "Standart", "Standart", "RW");

    private long lastTickTime = 0L;
    private int ticksTwo = 0;

    public GrimGlide() {
        super("Glide Fly", "Ускорение на элитре без фееров", ModuleCategory.MOVEMENT);
        addSettings(mode);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (!isGliding()) return;

        if (mode.is("Standart")) {
            handleGlide();
        }
    }

    @EventLink
    public void onTick(EventTickPre event) {
        if (!isGliding()) return;

        if (mode.is("RW")) {
            handleGlide();
        }
    }

    private void handleGlide() {
        ticksTwo++;

        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double forward = mc.player.age % 2 == 0 ? 0.087D : 0.09D;

        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;

        if (System.currentTimeMillis() - lastTickTime >= 40L) {
            mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
            lastTickTime = System.currentTimeMillis();
        }

        if (ticksTwo % 40 == 0) {
            mc.player.setVelocity(
                    dx * ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F),
                    mc.player.getVelocity().y + 0.00600000075995922D,
                    dz * ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F)
            );
        }
    }

    private boolean isGliding() {
        return mc.player != null && mc.world != null && mc.player.isGliding();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ticksTwo = 0;
        lastTickTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        ticksTwo = 0;
        super.onDisable();
    }
}
