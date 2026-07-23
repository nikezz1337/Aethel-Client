package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.util.player.other.InventoryUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInformation(
        moduleName = "AutoInvisible",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Авто-пить зелье невидимости"
)
public class AutoInvisible extends Module {

    public final BooleanSetting preDrink =
            new BooleanSetting("Пить заранее", false);

    private boolean isUsingPotion;

    public AutoInvisible() {
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        boolean hasInvisibility = mc.player.hasStatusEffect(StatusEffects.INVISIBILITY);
        StatusEffectInstance effect = hasInvisibility
                ? mc.player.getStatusEffect(StatusEffects.INVISIBILITY) : null;

        boolean shouldDrink = !hasInvisibility;
        if (preDrink.getValue() && effect != null && effect.getDuration() <= 200) {
            shouldDrink = true;
        }

        if (shouldDrink) {
            // Check offhand for invisibility potion
            boolean hasPotionInOffhand = isInvisibilityPotion(mc.player.getOffHandStack());

            // Find potion in inventory
            int potionSlot = findInvisibilityPotion();

            if (potionSlot != -1 && !hasPotionInOffhand) {
                // Move potion to offhand via swap
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        potionSlot, 40, SlotActionType.SWAP, mc.player);
            }

            if (hasPotionInOffhand || potionSlot != -1) {
                isUsingPotion = true;
                mc.options.useKey.setPressed(true);
            }
        } else if (isUsingPotion) {
            mc.options.useKey.setPressed(false);
            isUsingPotion = false;

            // Throw away empty bottle from offhand
            if (mc.player.getOffHandStack().getItem() == Items.GLASS_BOTTLE) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        45, 1, SlotActionType.THROW, mc.player);
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
