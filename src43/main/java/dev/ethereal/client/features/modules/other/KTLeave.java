package dev.ethereal.client.features.modules.other;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.client.features.modules.combat.AuraModule;
import lombok.Getter;
import net.minecraft.screen.slot.SlotActionType;

@ModuleRegister(name = "KTLeave", category = Category.OTHER)
public class KTLeave extends Module {
    @Getter
    private static final KTLeave instance = new KTLeave();

    private final BindSetting bind = new BindSetting("Бинд");

    public KTLeave() {
        addSettings(bind);
    }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1) return;
            if (event.key() == bind.getValue() && bind.getValue() != -1 && bind.getValue() != -999) {
                mc.interactionManager.clickSlot(
                        0, -999, 0, SlotActionType.PICKUP, mc.player);
            }
        }));

        addEvents(keyEvent);
    }
}
