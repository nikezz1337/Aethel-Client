package dev.ethereal.client.features.modules.movement;

import lombok.Getter;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.player.PlayerUtil;

@ModuleRegister(name = "No Web", category = Category.MOVEMENT)
public class NoWebModule extends Module {
    @Getter private static final NoWebModule instance = new NoWebModule();

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (PlayerUtil.isInWeb()) {
            mc.player.setVelocity(0, 0, 0);

            double verticalSpeed = 0.995;
            double horizantalSpeed = 0.19175;

            if (mc.options.jumpKey.isPressed()) {
                mc.player.getVelocity().y = verticalSpeed;
            } else if (mc.options.sneakKey.isPressed()) {
                mc.player.getVelocity().y = -verticalSpeed;
            } else if (!mc.player.isOnGround()) {
                mc.player.getVelocity().y = 0;
            }

            MoveUtil.setSpeed(horizantalSpeed);
        }
    }
}
