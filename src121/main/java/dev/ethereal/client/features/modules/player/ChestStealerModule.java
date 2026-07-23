package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.other.StopWatch;
import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.interfaces.QuickImports;

@ModuleRegister(name = "ChestStealer", category = Category.PLAYER)
public class ChestStealerModule extends Module implements QuickImports {
    @Getter private static final ChestStealerModule instance = new ChestStealerModule();

    private final SliderSetting delay = new SliderSetting("Задержка").value(100f).range(0f, 500f).step(10f);
    private final BooleanSetting autoClose = new BooleanSetting("Авто закрытие").value(true);
    private final BooleanSetting ignoreTrash = new BooleanSetting("Игнорировать мусор").value(false);

    TimerUtil timeClick = new TimerUtil();

    public ChestStealerModule() {
        addSettings(delay, autoClose, ignoreTrash);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        String title = screen.getTitle().getString();
        if (title.contains("Аукцион") || title.contains("Маркет") ||
            title.contains("Поиск") || title.contains("ꈁꀀꈂꌲꈂꀁ") || title.contains("Эндер") || title.contains("Ender")) {
            return;
        }

        if (!timeClick.finished(delay.getValue())) return;

        boolean foundItem = false;

        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.inventory == mc.player.getInventory()) continue;

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            if (ignoreTrash.getValue() && isTrash(stack)) continue;

            mc.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slot.id,
                0,
                SlotActionType.QUICK_MOVE,
                mc.player
            );

            timeClick.reset();
            foundItem = true;
            break;
        }

        if (!foundItem && autoClose.getValue()) {
            mc.player.closeHandledScreen();
        }
    }

    private boolean isTrash(ItemStack stack) {
        String name = stack.getName().getString().toLowerCase();
        return name.contains("стрела") || 
               name.contains("arrow") ||
               name.contains("семена") ||
               name.contains("seeds") ||
               name.contains("удочка") ||
               name.contains("fishing rod");
    }
}
