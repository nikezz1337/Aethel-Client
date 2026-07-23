package dev.ethereal.api.utils.auction.ab;

import dev.ethereal.api.system.interfaces.QuickImports;
import lombok.experimental.UtilityClass;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

@UtilityClass
public class ItemMatcher implements QuickImports {

    public boolean matchesByTooltip(ItemStack stack, String... keywords) {
        if (stack.isEmpty()) return false;
        
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
        
        for (String keyword : keywords) {
            boolean found = false;
            for (Text line : tooltip) {
                if (line.getString().contains(keyword)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        
        return true;
    }

    public boolean matchesByTooltipAny(ItemStack stack, String... keywords) {
        if (stack.isEmpty()) return false;
        
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
        
        for (String keyword : keywords) {
            for (Text line : tooltip) {
                if (line.getString().contains(keyword)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public boolean containsInLore(ItemStack stack, String text) {
        if (stack.isEmpty()) return false;
        
        NbtComponent nbt = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbt == null) return false;
        
        var root = nbt.copyNbt();
        if (!root.contains("display")) return false;
        
        var display = root.getCompound("display");
        if (!display.contains("Lore")) return false;
        
        var lore = display.getList("Lore", 8);
        for (int i = 0; i < lore.size(); i++) {
            if (lore.getString(i).toLowerCase().contains(text.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    public boolean matchesDisplayName(ItemStack stack, String name) {
        return stack.getName().getString().contains(name);
    }
}
