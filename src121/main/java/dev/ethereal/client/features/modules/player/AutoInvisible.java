package dev.ethereal.client.features.modules.player;

import com.google.common.eventbus.Subscribe;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import lombok.Getter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

@ModuleRegister(
        name = "AutoInvisible",
        category = Category.PLAYER
)
public class AutoInvisible extends Module {
    @Getter
    private static final AutoInvisible instance = new AutoInvisible();

    public final BooleanSetting preDrink =
            new BooleanSetting("Пить заранее").value(false);

    private boolean isUsingPotion;

    public AutoInvisible() {
        addSettings(preDrink);
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        boolean hasInvisibility = mc.player.hasStatusEffect(StatusEffects.INVISIBILITY);
        StatusEffectInstance effect = hasInvisibility
                ? mc.player.getStatusEffect(StatusEffects.INVISIBILITY) : null;

        boolean shouldDrink = !hasInvisibility;
        if (preDrink.getValue() && effect != null && effect.getDuration() <= 200) {
            shouldDrink = true;
        }

        if (shouldDrink) {
            boolean hasPotionInOffhand = isInvisibilityPotion(mc.player.getOffHandStack());

            int potionSlot = findInvisibilityPotion();

            if (potionSlot != -1 && !hasPotionInOffhand) {
                SlownessManager.applySlowness(10, () -> {
                    mc.interactionManager.clickSlot(
                            mc.player.currentScreenHandler.syncId,
                            potionSlot, 40, SlotActionType.SWAP, mc.player);
                });
            }

            if (hasPotionInOffhand || potionSlot != -1) {
                isUsingPotion = true;
                mc.options.useKey.setPressed(true);
            }
        } else if (isUsingPotion) {
            mc.options.useKey.setPressed(false);
            isUsingPotion = false;

            if (mc.player.getOffHandStack().getItem() == Items.GLASS_BOTTLE) {
                SlownessManager.applySlowness(10, () -> {
                    mc.interactionManager.clickSlot(
                            mc.player.currentScreenHandler.syncId,
                            45, 1, SlotActionType.THROW, mc.player);
                });
            }
        }
    }

    private boolean isInvisibilityPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == Items.SPLASH_POTION
                || stack.getItem() == Items.LINGERING_POTION;
    }

    private int findInvisibilityPotion() {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (!stack.isEmpty() && (stack.getItem() == Items.SPLASH_POTION
                    || stack.getItem() == Items.LINGERING_POTION)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        isUsingPotion = false;
        if (mc.player != null) {
            mc.options.useKey.setPressed(false);
        }
        super.onDisable();
    }
}
