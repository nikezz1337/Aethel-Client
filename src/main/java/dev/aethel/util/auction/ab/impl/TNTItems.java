package dev.aethel.util.auction.ab.impl;

import dev.aethel.util.auction.ab.ABItems;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;

@UtilityClass
public class TNTItems {
    public ItemStack getTierBlack() { return ABItems.tierBlack(); }
    public ItemStack getTierWhite() { return ABItems.tierWhite(); }
}
