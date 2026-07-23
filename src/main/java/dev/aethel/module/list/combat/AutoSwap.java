package dev.aethel.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import dev.aethel.event.list.EventKeyInput;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BindSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "Auto Swap", moduleCategory = ModuleCategory.COMBAT, moduleDesc = "Авто-свап предметов в руках")
public class AutoSwap extends Module {
    private final BindSetting swapKey = new BindSetting("Клавиша свапа", -1);
    private final ModeSetting firstItem = new ModeSetting("Первый предмет", "Шар", "Гепл", "Щит", "Талисман", "Шар");
    private final ModeSetting secondItem = new ModeSetting("Второй предмет", "Шар", "Гепл", "Щит", "Талисман", "Шар");

    private boolean swapped;

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (swapped) {
            swapped = false;
            boolean sameItem = firstItem.getValue().equals(secondItem.getValue());

            int slotFirstItem = findItemByName(firstItem.getValue(), sameItem);
            int slotSecondItem = findItemByName(secondItem.getValue(), sameItem);

            if (slotFirstItem == -1 && slotSecondItem == -1) return;

            int syncId = mc.player.currentScreenHandler.syncId;

            if (slotFirstItem == 40 || (slotFirstItem == -1 && slotSecondItem != 40)) {
                performSwap(syncId, slotSecondItem, 40);
            } else {
                if (slotFirstItem == -1) return;
                performSwap(syncId, slotFirstItem, 40);
            }
        }
    }

    private void performSwap(int syncId, int fromSlot, int toSlot) {
        if (fromSlot == -1) return;

        int screenFrom = fromSlot == 40 ? 45 : InventoryUtil.toScreenSlot(fromSlot);
        int button = toSlot == 40 ? 40 : InventoryUtil.toScreenSlot(toSlot);

        InventoryUtil.swapBypass(() ->
                mc.interactionManager.clickSlot(syncId, screenFrom, button, SlotActionType.SWAP, mc.player));
    }

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (e.getAction() == 0) return;
        if (e.getKey() == swapKey.getValue()) swapped = true;
    }

    private int findItemByName(String name, boolean ignoreOffhand) {
        switch (name) {
            case "Гепл" -> {
                if (!ignoreOffhand && mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE)
                    return 40;
                return InventoryUtil.searchItem(Items.GOLDEN_APPLE);
            }
            case "Щит" -> {
                if (!ignoreOffhand && mc.player.getOffHandStack().getItem() == Items.SHIELD)
                    return 40;
                return InventoryUtil.searchItem(Items.SHIELD);
            }
            case "Талисман" -> {
                if (!ignoreOffhand &&
                        mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING &&
                        mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null &&
                        !mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty())
                    return 40;
                return InventoryUtil.searchItemStack(item ->
                        item.getItem() == Items.TOTEM_OF_UNDYING &&
                                item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null &&
                                !item.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty()
                );
            }
            case "Шар" -> {
                if (!ignoreOffhand &&
                        mc.player.getOffHandStack().getItem() == Items.PLAYER_HEAD &&
                        mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null &&
                        !mc.player.getOffHandStack().get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty())
                    return 40;
                return InventoryUtil.searchItemStack(item ->
                        item.getItem() == Items.PLAYER_HEAD
                );
            }
        }
        return -1;
    }
}
