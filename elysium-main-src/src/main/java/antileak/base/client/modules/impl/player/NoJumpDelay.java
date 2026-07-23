package antileak.base.client.modules.impl.player;

import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.client.modules.Module;
import antileak.base.mixin.ILivingEntity;

public class NoJumpDelay extends Module {

    public static NoJumpDelay INSTANCE = new NoJumpDelay();

    public NoJumpDelay() {
        super("NoJumpDelay", "Убирает задержку на прыжок", ModuleCategory.PLAYER);
    }

    @EventLink
    public void onEvent(final EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        ((ILivingEntity) mc.player).setJumpingCooldown(0);
    }
}
