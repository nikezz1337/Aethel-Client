package dev.aethel.module.list.combat.aura.rotation;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.LookEvent;
import dev.aethel.event.list.RotationEvent;
import net.minecraft.util.math.MathHelper;

public class FreeLookComponent extends Component {

    public static boolean active;
    public static float freeYaw, freePitch;

    public static boolean isActive() { return active; }
    public static void setActive(boolean a) { active = a; }
    public static float getFreeYaw() { return freeYaw; }
    public static void setFreeYaw(float y) { freeYaw = y; }
    public static float getFreePitch() { return freePitch; }
    public static void setFreePitch(float p) { freePitch = p; }

    @Subscribe
    public void onEvent(LookEvent event) {
        if (active) {
            rotateTowards(event.getYaw(), event.getPitch());
            event.cancelEvent();
        }
    }

    @Subscribe
    public void onEvent(RotationEvent event) {
        if (active) {
            event.setYaw(freeYaw);
            event.setPitch(freePitch);
        } else {
            freeYaw = event.getYaw();
            freePitch = event.getPitch();
        }
    }

    private void rotateTowards(double targetYaw, double targetPitch) {
        freePitch = MathHelper.clamp((float) (freePitch + targetPitch * 0.15D), -90.0F, 90.0F);
        freeYaw = (float) (freeYaw + targetYaw * 0.15D);
    }
}
