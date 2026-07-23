package dev.aethel.util.auction.ab;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public class BuyableItem {
    @Getter
    private final String name;
    @Getter
    private final Item displayItem;
    private final Predicate<ItemStack> checker;
    @Setter
    @Getter
    private int maxPrice;
    @Setter
    @Getter
    private boolean enabled;

    public BuyableItem(String name, Item displayItem, Predicate<ItemStack> checker) {
        this.name = name;
        this.displayItem = displayItem;
        this.checker = checker;
        this.maxPrice = 0;
        this.enabled = true;
    }

    public boolean matches(ItemStack stack) {
        return enabled && checker.test(stack);
    }
}
