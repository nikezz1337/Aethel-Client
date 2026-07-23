package dev.aethel.util.inventory;

import net.minecraft.item.ItemStack;

public record InventoryResult(int slot, boolean found, ItemStack stack) {
    public static final InventoryResult NOT_FOUND = new InventoryResult(-1, false, ItemStack.EMPTY);

    public static InventoryResult notFound() {
        return NOT_FOUND;
    }
}
