package dev.aethel.util.item;

import lombok.experimental.UtilityClass;
import net.minecraft.item.*;
import dev.aethel.util.IMinecraft;

@UtilityClass
public class ItemUtil implements IMinecraft {

    public int maxUseTick(Item item) {
        return maxUseTick(item.getDefaultStack());
    }

    public int maxUseTick(ItemStack stack) {
        return switch (stack.getUseAction()) {
            case EAT, DRINK -> 32;
            case CROSSBOW, SPEAR -> 10;
            case BOW -> 20;
            case BLOCK -> 0;
            default -> stack.getMaxUseTime(mc.player);
        };
    }

    public float getCooldownProgress(Item item) {
        if (mc.player == null) return 0;
        return mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack()) ? 1f : 0f;
    }
}
