package dev.ethereal.client.features.modules.movement;

import lombok.Getter;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.other.RotationUpdateEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.rotation.RotationChanger;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import dev.ethereal.api.utils.rotation.manager.RotationPlan;

@ModuleRegister(name = "Strafe", category = Category.MOVEMENT)
public class StrafeModule extends Module {
    @Getter private static final StrafeModule instance = new StrafeModule();

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        int[] ticks = {1};
        RotationManager.getInstance().addRotation(new RotationChanger(
                1,
                () -> new Float[]{mc.player.getYaw() + dony(), mc.player.getPitch()},
                () -> --ticks[0] <= 0
        ));
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        RotationPlan currentPlan = RotationManager.getInstance().getCurrentRotationPlan();
        if (currentPlan != null && currentPlan.provider() != this) return;

        boolean w = MoveUtil.w();
        boolean s = MoveUtil.s();
        boolean a = MoveUtil.a();
        boolean d = MoveUtil.d();

        if (w && s) w = s = false;
        if (a && d) a = d = false;

        event.getDirectionalInput().setLeft(false);
        event.getDirectionalInput().setRight(false);
        event.getDirectionalInput().setBackwards(false);
        event.getDirectionalInput().setForwards(w || s || a || d);
    }

    private float dony() {
        boolean w = MoveUtil.w();
        boolean s = MoveUtil.s();
        boolean a = MoveUtil.a();
        boolean d = MoveUtil.d();

        if (w && s) {
            w = false;
            s = false;
        }

        if (a && d) {
            a = false;
            d = false;
        }

        if (w) {
            if (a) return -45f;
            if (d) return 45f;
            return 0f;
        }

        if (s) {
            if (a) return -135f;
            if (d) return 135f;
            return 180f;
        }

        if (a) return -90f;
        if (d) return 90f;

        return 0f;
    }
}
