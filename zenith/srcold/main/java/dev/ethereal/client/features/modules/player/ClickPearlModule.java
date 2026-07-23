package dev.ethereal.client.features.modules.player;

import lombok.Getter;
import net.minecraft.item.Items;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
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

    private final BindSetting throwKey = new BindSetting("Throw key").value(-999);
    private final BooleanSetting legit = new BooleanSetting("Legit").value(false);

    private final InventoryUtil.ItemUsage itemUsage = new InventoryUtil.ItemUsage(Items.ENDER_PEARL, this);

    public ClickPearlModule() {
        addSettings(throwKey, legit);
    }

    @Override
    public void onDisable() {
        itemUsage.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handle(!SlownessManager.isEnabled());
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handle(SlownessManager.isEnabled());
        }));

        addEvents(tickEvent, updateEvent);
    }

    private void handle(boolean tick) {
        if (tick) return;

        itemUsage.handleUse(throwKey.getValue(), legit.getValue());
    }
}
