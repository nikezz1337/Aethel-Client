package antileak.base.client.modules.impl.movement;

import antileak.base.elysium;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.impl.combat.Aura;
import antileak.base.client.modules.settings.implement.BooleanSetting;

public class AutoJump extends Module {

    public static AutoJump INSTANCE = new AutoJump();

    public AutoJump() {
        super("AutoJump","Прыгает автоматически при ауре", ModuleCategory.MOVEMENT);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        Aura aura = ModuleClass.INSTANCE.aura;

        if (aura == null || !aura.isEnable()) return;

        if (aura.getTarget() != null) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }
}
