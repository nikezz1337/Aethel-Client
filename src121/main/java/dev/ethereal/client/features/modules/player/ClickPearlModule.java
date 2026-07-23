package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.features.modules.movement.InventoryMoveModule;
import lombok.Getter;
import net.minecraft.item.Items;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.other.SlownessManager;

@ModuleRegister(name = "Click Pearl", category = Category.PLAYER)
public class ClickPearlModule extends Module {
    @Getter private static final ClickPearlModule instance = new ClickPearlModule();

    private final BindSetting throwKey = new BindSetting("Кнопка броска").value(-999);

    private final InventoryUtil.ItemUsage itemUsage = new InventoryUtil.ItemUsage(Items.ENDER_PEARL, this);

    boolean packet;

    public ClickPearlModule() {
        addSettings(throwKey);
    }

    @Override
    public void onDisable() {
        itemUsage.onDisable();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        handle(!SlownessManager.isEnabled());
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        packet = PlayerUtil.isHW() || PlayerUtil.isST();
        handle(SlownessManager.isEnabled());
    }

    private void handle(boolean tick) {
        if (tick) return;

        itemUsage.handleUse(throwKey.getValue(), !packet && InventoryMoveModule.getInstance().isLegit());
    }
}
