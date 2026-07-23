package fun.wonderful.client.modules.impl.movement;

import fun.wonderful.Wonderful;
import fun.wonderful.api.events.EventLink;
import fun.wonderful.api.events.implement.EventUpdate;
import fun.wonderful.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import fun.wonderful.client.modules.Module;
import fun.wonderful.client.modules.impl.combat.Aura;
import fun.wonderful.client.modules.settings.implement.BooleanSetting;

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
