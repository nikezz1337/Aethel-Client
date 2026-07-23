package antileak.base.client.modules.impl.misc;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventBinding;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.utils.player.InventoryUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BindSetting;
import antileak.base.client.modules.settings.implement.BooleanSetting;

public class ClickPearl extends Module {

    public static ClickPearl INSTANCE = new ClickPearl();

    private final BindSetting keyToPearl = new BindSetting("Кнопка", -1);
    private final BooleanSetting bypass = new BooleanSetting("Обход", true);

    private boolean use;

    public ClickPearl() {
        super("ClickPearl", "Кидает перку по внутреннему бинду", ModuleCategory.MISC);
        addSettings(keyToPearl, bypass);
    }

    @Override
    public void onEnable() {
        this.use = false;
        super.onEnable();
    }

    @EventLink
    public void onEvent(final EventBinding event) {
        if (mc.currentScreen != null) return;
        if (event.getKey() == keyToPearl.getKey()) {
            this.use = true;
        }
    }

    @EventLink
    public void onEvent(final EventUpdate event) {
        if (!this.use) return;
        if (mc.player == null || mc.world == null) {
            this.use = false;
            return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        int pearlSlot = InventoryUtils.find(Items.ENDER_PEARL, 0, 36);

        if (pearlSlot > 9 && this.use) {
            mc.player.setSprinting(false);
        }

        if (pearlSlot == -1) {
            this.use = false;
            return;
        }


        if (bypass.isState()) {
            mc.player.getInventory().selectedSlot = pearlSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = oldSlot;
        } else {
            InventoryUtils.swapDef(Items.ENDER_PEARL);
        }

        this.use = false;
    }
}