package dev.ethereal.client.features.modules.other.autobuy;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class AutoBuyItemSettings {
    private int buyBelow;
    private int sellAbove;
    private int minQuantity;
    private final boolean canHaveQuantity;
    private final String itemName;

    public AutoBuyItemSettings(int defaultBuyBelow, Item material, String itemName) {
        this.itemName = itemName;
        this.buyBelow = defaultBuyBelow;
        this.sellAbove = (int) (defaultBuyBelow * 1.5);
        this.minQuantity = 1;
        this.canHaveQuantity = canItemStack(material);
    }

    private boolean canItemStack(Item material) {
        if (material == Items.NETHERITE_HELMET || material == Items.NETHERITE_CHESTPLATE ||
                material == Items.NETHERITE_LEGGINGS || material == Items.NETHERITE_BOOTS ||
                material == Items.NETHERITE_SWORD || material == Items.NETHERITE_PICKAXE ||
                material == Items.CROSSBOW || material == Items.TRIDENT || material == Items.MACE ||
                material == Items.ELYTRA || material == Items.TOTEM_OF_UNDYING) {
            return false;
        }
        return material.getMaxCount() > 1;
    }

    public int getBuyBelow() { return buyBelow; }
    public void setBuyBelow(int buyBelow) { this.buyBelow = buyBelow; }
    public int getSellAbove() { return sellAbove; }
    public void setSellAbove(int sellAbove) { this.sellAbove = sellAbove; }
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public boolean isCanHaveQuantity() { return canHaveQuantity; }
    public String getItemName() { return itemName; }
}
