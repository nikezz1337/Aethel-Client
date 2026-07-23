package dev.aethel.util;

import net.minecraft.item.ItemStack;

public record InventoryResult(int slot, boolean found, ItemStack stack) implements IMinecraft {
    private static final InventoryResult NOT_FOUND_RESULT = new InventoryResult(-1, false, ItemStack.EMPTY);

    public static InventoryResult notFound() {
        return NOT_FOUND_RESULT;
    }

    public static InventoryResult inOffhand(ItemStack stack) {
        return new InventoryResult(999, true, stack);
    }

    public boolean isHolding() {
        return mc.player != null && mc.player.getInventory().selectedSlot == slot;
    }

    public boolean isInHotBar() {
        return slot >= 0 && slot < 9;
    }

    public void switchTo() {
        if (found && isInHotBar()) InventoryToolkit.switchTo(slot);
    }

    public void switchToSilent() {
        if (found && isInHotBar()) InventoryToolkit.switchToSilent(slot);
    }
}
