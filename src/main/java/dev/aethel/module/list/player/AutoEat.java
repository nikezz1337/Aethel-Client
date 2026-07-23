package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleInformation(
    moduleName = "AutoEat",
    moduleCategory = ModuleCategory.PLAYER,
    moduleDesc = "Автоматически ест при низком голоде"
)
public class AutoEat extends Module {

    public static AutoEat INSTANCE;

    private final SliderSetting hungerBars = new SliderSetting("Плашки голода", 6.0, 1.0, 10.0, 1.0);

    private boolean eating;
    private boolean swappedFromInventory;
    private int originalSlot = -1;
    private int swappedInventorySlot = -1;

    public AutoEat() {
        INSTANCE = this;
    }

    public static boolean shouldSuppressCombat() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.eating;
    }

    @Override
    public void onDisable() {
        stopEating();
        super.onDisable();
    }

    @Subscribe
    public void onTick(EventTick event) {
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
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            stopEating();
            return;
        }

        mc.options.attackKey.setPressed(false);

        if (!needsFood()) {
            if (!player.isUsingItem()) stopEating();
            return;
        }

        if (!ensureFoodReady()) {
            stopEating();
            return;
        }

        Hand eatingHand = getEatingHand(player);
        if (eatingHand == null) {
            stopEating();
            return;
        }

        mc.options.useKey.setPressed(true);

        if (!player.isUsingItem() || player.getActiveHand() != eatingHand) {
            mc.interactionManager.interactItem(player, eatingHand);
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
        return Math.round(hungerBars.getFloatValue()) * 2;
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
            selectHotbarSlot(foodSlot);
            return isValidFood(player.getMainHandStack());
        }

        selectHotbarSlot(originalSlot == -1 ? player.getInventory().selectedSlot : originalSlot);
        swapInventorySlotWithHotbar(foodSlot, player.getInventory().selectedSlot);
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
        ClientPlayerEntity player = mc.player;
        if (player == null) return -1;

        int selected = player.getInventory().selectedSlot;
        if (isValidFood(player.getInventory().getStack(selected))) return selected;

        for (int slot = 0; slot < 9; slot++) {
            if (slot == selected) continue;
            if (isValidFood(player.getInventory().getStack(slot))) return slot;
        }

        for (int slot = 9; slot < 36; slot++) {
            if (isValidFood(player.getInventory().getStack(slot))) return slot;
        }

        return -1;
    }

    private boolean isValidFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || stack.isOf(Items.CHORUS_FRUIT)) return false;
        return stack.getUseAction() == UseAction.EAT;
    }

    private void selectHotbarSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8 || mc.player.getInventory().selectedSlot == slot) return;
        mc.player.getInventory().selectedSlot = slot;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void swapInventorySlotWithHotbar(int inventorySlot, int hotbarSlot) {
        if (mc.player == null || mc.interactionManager == null || inventorySlot < 9 || inventorySlot > 35 || hotbarSlot < 0 || hotbarSlot > 8) return;
        mc.interactionManager.clickSlot(0, inventorySlot, hotbarSlot, SlotActionType.SWAP, mc.player);
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
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
            selectHotbarSlot(hotbarSlot);
            swapInventorySlotWithHotbar(swappedInventorySlot, hotbarSlot);
        }

        if (originalSlot != -1) selectHotbarSlot(originalSlot);
        resetSwapState();
    }

    private void resetSwapState() {
        swappedFromInventory = false;
        swappedInventorySlot = -1;
        originalSlot = -1;
    }
}
