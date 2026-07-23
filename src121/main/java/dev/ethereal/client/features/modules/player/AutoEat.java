package dev.ethereal.client.features.modules.player;

import com.google.common.eventbus.Subscribe;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.client.features.modules.combat.Aura;
import lombok.Getter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleRegister(name = "AutoEat", category = Category.PLAYER)
public class AutoEat extends Module {
    @Getter
    private static final AutoEat instance = new AutoEat();

    private final SliderSetting hungerBars = new SliderSetting("Уровень голода").value(6.0f).range(1.0f, 10.0f).step(1);

    private boolean eating;
    private boolean swappedFromInventory;
    private int originalSlot = -1;
    private int swappedInventorySlot = -1;

    public AutoEat() {
      addSettings(hungerBars);
    }

    @Override
    public void onDisable() {
        stopEating();
        super.onDisable();
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            stopEating();
            return;
        }

        if (mc.currentScreen != null) {
            stopEating();
            return;
        }

        if (mc.player.getAbilities().creativeMode || mc.player.isSpectator()) {
            stopEating();
            return;
        }

        if (!eating) {
            if (!shouldStartEating()) return;
            eating = true;
            originalSlot = mc.player.getInventory().selectedSlot;
        }

        tickEating();
    }

    private void tickEating() {
        if (mc.player == null) {
            stopEating();
            return;
        }

        mc.options.attackKey.setPressed(false);

        if (!needsFood()) {
            if (!mc.player.isUsingItem()) stopEating();
            return;
        }

        if (!ensureFoodReady()) {
            stopEating();
            return;
        }

        Hand eatingHand = getEatingHand(mc.player);
        if (eatingHand == null) {
            stopEating();
            return;
        }

        mc.options.useKey.setPressed(true);

        if (!mc.player.isUsingItem() || mc.player.getActiveHand() != eatingHand) {
            mc.interactionManager.interactItem(mc.player, eatingHand);
        }
    }

    private boolean shouldStartEating() {
        return needsFood() && !mc.player.isUsingItem() && (isValidFood(mc.player.getOffHandStack()) || findFoodSlot() != -1);
    }

    private boolean needsFood() {
        return mc.player != null
                && mc.player.getHungerManager().getFoodLevel() < 20
                && mc.player.getHungerManager().getFoodLevel() <= getFoodThreshold();
    }

    private int getFoodThreshold() {
        return Math.round(hungerBars.getValue() * 2);
    }

    private boolean ensureFoodReady() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return false;

        if (isValidFood(player.getOffHandStack())) return true;
        if (isValidFood(player.getMainHandStack())) return true;

        int foodSlot = findFoodSlot();
        if (foodSlot == -1) return false;

        if (foodSlot < 9) {
            swappedFromInventory = false;
            swappedInventorySlot = -1;
            InventoryUtil.swapToSlot(foodSlot);
            return isValidFood(player.getMainHandStack());
        }

        InventoryUtil.swapToSlot(originalSlot == -1 ? player.getInventory().selectedSlot : originalSlot);
        SlownessManager.applySlowness(10, () -> {
            swap(foodSlot, player.getInventory().selectedSlot);
        });
        swappedFromInventory = true;
        swappedInventorySlot = foodSlot;
        return isValidFood(player.getMainHandStack());
    }

    private Hand getEatingHand(ClientPlayerEntity player) {
        if (player == null) return null;
        if (isValidFood(player.getOffHandStack())) return Hand.OFF_HAND;
        if (isValidFood(player.getMainHandStack())) return Hand.MAIN_HAND;
        return null;
    }

    private int findFoodSlot() {
        if (mc.player == null) return -1;

        int selected = mc.player.getInventory().selectedSlot;
        if (isValidFood(mc.player.getInventory().getStack(selected))) return selected;

        for (int slot = 0; slot < 9; slot++) {
            if (slot == selected) continue;
            if (isValidFood(mc.player.getInventory().getStack(slot))) return slot;
        }

        for (int slot = 9; slot < 36; slot++) {
            if (isValidFood(mc.player.getInventory().getStack(slot))) return slot;
        }

        return -1;
    }

    private boolean isValidFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || stack.isOf(Items.CHORUS_FRUIT)) return false;
        return stack.getUseAction() == UseAction.EAT;
    }

    private void swap(int inventorySlot, int hotbarSlot) {
        if (mc.player == null || mc.interactionManager == null || inventorySlot < 9 || inventorySlot > 35 || hotbarSlot < 0 || hotbarSlot > 8) return;
        mc.interactionManager.clickSlot(0, inventorySlot, hotbarSlot, SlotActionType.SWAP, mc.player);
        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
    }

    private void stopEating() {
        if (mc.options != null) mc.options.useKey.setPressed(false);
        restoreHeldItem();
        eating = false;
    }

    private void restoreHeldItem() {
        if (mc.player == null || mc.interactionManager == null) {
            resetSwapState();
            return;
        }

        if (swappedFromInventory && swappedInventorySlot != -1) {
            int hotbarSlot = originalSlot == -1 ? mc.player.getInventory().selectedSlot : originalSlot;
            InventoryUtil.swapToSlot(hotbarSlot);
            SlownessManager.applySlowness(10L, () -> {
                swap(swappedInventorySlot, hotbarSlot);
            });
        }

        if (originalSlot != -1) InventoryUtil.swapToSlot(originalSlot);
        resetSwapState();
    }

    private void resetSwapState() {
        swappedFromInventory = false;
        swappedInventorySlot = -1;
        originalSlot = -1;
    }
}
