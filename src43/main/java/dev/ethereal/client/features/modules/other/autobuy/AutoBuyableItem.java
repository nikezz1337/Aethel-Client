package dev.ethereal.client.features.modules.other.autobuy;

import dev.ethereal.client.features.modules.other.autobuy.AutoBuyItemSettings;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;

public interface AutoBuyableItem {
    String getDisplayName();
    String getSearchName();
    ItemStack createItemStack();
    int getPrice();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    AutoBuyItemSettings getSettings();

    default Item getItem() {
        return createItemStack().getItem();
    }

    default boolean needsAdditionalCheck() {
        ItemStack stack = createItemStack();
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyNbt();
            if (nbt != null) {
                if (nbt.getBoolean("FunTimeSphere") ||
                    nbt.getBoolean("FunTimeTalik") ||
                    nbt.getBoolean("FunTimePotion") ||
                    nbt.getBoolean("FunTimeSpecial")) {
                    return true;
                }
                if (nbt.contains("funItemType", NbtElement.STRING_TYPE)) {
                    String type = nbt.getString("funItemType");
                    if (type != null && !type.isEmpty()) return true;
                }
                if (nbt.contains("AttributeModifiers", NbtElement.LIST_TYPE)) {
                    var attrs = nbt.getList("AttributeModifiers", NbtElement.LIST_TYPE);
                    if (!attrs.isEmpty()) return true;
                }
                if (nbt.contains("RequiredEnchantments", NbtElement.LIST_TYPE)) {
                    var enchants = nbt.getList("RequiredEnchantments", NbtElement.LIST_TYPE);
                    if (!enchants.isEmpty()) return true;
                }
            }
        }
        var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) return true;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null && !lore.lines().isEmpty()) return true;
        var potion = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potion != null && potion.getEffects() != null && potion.getEffects().iterator().hasNext()) return true;
        return false;
    }
}
