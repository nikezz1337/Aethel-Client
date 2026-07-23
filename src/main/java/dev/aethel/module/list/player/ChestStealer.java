package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(
    moduleName = "ChestStealer",
    moduleCategory = ModuleCategory.PLAYER,
    moduleDesc = "Автоматически забирает предметы из сундуков"
)
public class ChestStealer extends Module {

    private final SliderSetting stealDelay = new SliderSetting("Задержка", 100.0, 0.0, 1000.0, 1.0);
    private final BooleanSetting randomize = new BooleanSetting("Рандомизация", false);

    private long lastStealTime = 0L;

    @Override
    public void onDisable() {
        lastStealTime = 0L;
        super.onDisable();
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.interactionManager == null) return;

        ScreenHandler openContainer = mc.player.currentScreenHandler;
        if (openContainer == null || openContainer == mc.player.playerScreenHandler) return;

        if (!(openContainer instanceof GenericContainerScreenHandler) && !(openContainer instanceof HopperScreenHandler)) return;

        long currentTime = System.currentTimeMillis();
        long delay = (long) stealDelay.getValue();
        if (currentTime - lastStealTime < delay) return;

        List<Slot> slots = openContainer.slots;
        findValidItem(slots, openContainer).ifPresent(slot -> {
            if (mc.player.currentScreenHandler == openContainer) {
                mc.interactionManager.clickSlot(openContainer.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                lastStealTime = currentTime;
            }
        });
    }

    private Optional<Slot> findValidItem(List<Slot> slots, ScreenHandler handler) {
        int containerSlotCount = getContainerSlotCount(handler);
        if (containerSlotCount <= 0 || containerSlotCount > slots.size()) return Optional.empty();

        List<Slot> containerSlots = slots.subList(0, containerSlotCount);
        List<Slot> validSlots = new ArrayList<>();

        for (Slot slot : containerSlots) {
            if (slot.hasStack() && !slot.getStack().isEmpty()) {
                if (!mc.player.getItemCooldownManager().isCoolingDown(slot.getStack())) {
                    validSlots.add(slot);
                }
            }
        }

        if (validSlots.isEmpty()) return Optional.empty();

        if (randomize.getValue()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(validSlots.size());
            return Optional.of(validSlots.get(randomIndex));
        } else {
            return Optional.of(validSlots.get(0));
        }
    }

    private int getContainerSlotCount(ScreenHandler handler) {
        if (handler instanceof GenericContainerScreenHandler container) {
            Inventory inventory = container.getInventory();
            return inventory.size();
        } else if (handler instanceof HopperScreenHandler) {
            return 5;
        }
        return 0;
    }
}
