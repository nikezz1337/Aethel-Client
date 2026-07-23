package dev.ethereal.api.utils.player;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.inject.accessors.ClientPlayerInteractionManagerAccessor;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class InventoryToolkit implements QuickImports {
    private static int cachedSlot = -1;

    private InventoryToolkit() {
    }

    public static int getSyncId() {
        if (mc.player == null) return 0;
        if (mc.player.currentScreenHandler != null) return mc.player.currentScreenHandler.syncId;
        if (mc.player.playerScreenHandler != null) return mc.player.playerScreenHandler.syncId;
        return 0;
    }

    public static Optional<Integer> findItem(Predicate<ItemStack> stackPredicate, boolean searchInInventory, boolean hotbarSlot) {
        if (mc.player == null) return Optional.empty();
        int inventorySize = searchInInventory ? mc.player.getInventory().main.size() : 9;
        for (int i = 0; i < inventorySize; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stackPredicate.test(stack)) {
                if (searchInInventory && !hotbarSlot) return Optional.of(i < 9 ? i + 36 : i);
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public static int getItemSlot(Item input) {
        if (mc.player == null) return -1;
        for (ItemStack stack : mc.player.getArmorItems()) {
            if (stack.isOf(input) && stack.getDamage() < 430) return -2;
        }
        int slot = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(input) && stack.getDamage() < 430) {
                slot = i;
                break;
            }
        }
        return slot < 9 && slot != -1 ? slot + 36 : slot;
    }

    public static InventoryResult findInInventory(Searcher searcher) {
        if (mc.player != null) {
            for (ItemStack stack : mc.player.getInventory().armor) {
                if (searcher.isValid(stack) && stack.getDamage() < 430) {
                    return new InventoryResult(-2, true, stack);
                }
            }
            for (int i = 36; i >= 0; i--) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (searcher.isValid(stack) && stack.getDamage() < 430) {
                    return new InventoryResult(i < 9 ? i + 36 : i, true, stack);
                }
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInInventory(List<Item> items) {
        return findInInventory(stack -> items.contains(stack.getItem()));
    }

    public static InventoryResult findItemInInventory(Item... items) {
        return findItemInInventory(Arrays.asList(items));
    }

    public static int getAxe() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    public static InventoryResult findItemInHotBar(Item item) {
        if (mc.player == null) return InventoryResult.notFound();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(item) && stack.getDamage() < 430) {
                return new InventoryResult(i, true, stack);
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInInventory(Item item) {
        return findInInventory(stack -> stack.isOf(item));
    }

    public static InventoryResult findInHotBar(Searcher searcher) {
        if (mc.player != null) {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (searcher.isValid(stack) && stack.getDamage() < 430) {
                    return new InventoryResult(i, true, stack);
                }
            }
        }
        return InventoryResult.notFound();
    }

    public static InventoryResult findItemInHotBar(List<Item> items) {
        return findInHotBar(stack -> items.contains(stack.getItem()));
    }

    public static InventoryResult findItemInHotBar(Item... items) {
        return findItemInHotBar(Arrays.asList(items));
    }

    public static void saveSlot() {
        if (mc.player != null) cachedSlot = mc.player.getInventory().selectedSlot;
    }

    public static void returnSlot() {
        if (cachedSlot != -1) switchTo(cachedSlot);
        cachedSlot = -1;
    }

    public static void switchTo(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;
        if (slot < 0 || slot > 8) return;
        if (mc.player.getInventory().selectedSlot == slot) return;
        mc.player.getInventory().selectedSlot = slot;
        if (mc.interactionManager instanceof ClientPlayerInteractionManagerAccessor accessor) {
            accessor.ethereal$syncSelectedSlot();
        } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    public static void switchToSilent(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (slot < 0 || slot > 8) return;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void interactItem(Hand hand) {
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.interactItem(mc.player, hand);
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.interactionManager == null || mc.world == null) return;
        if (mc.interactionManager instanceof ClientPlayerInteractionManagerAccessor accessor) {
            accessor.ethereal$sendSequencedPacket(mc.world, packetCreator);
            return;
        }
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(packetCreator.predict(0));
    }

    public static void sendNetworkPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendPacket(packet);
    }

    public static void clickSlot(int id) {
        clickSlot(id, 0, SlotActionType.PICKUP);
    }

    public static void clickSlot(int id, SlotActionType type) {
        clickSlot(id, 0, type);
    }

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id < 0 || type == null || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(getSyncId(), id, button, type, mc.player);
    }

    public static void swapToOffhand(int slotId) {
        if (slotId == -1 || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(getSyncId(), slotId, 40, SlotActionType.SWAP, mc.player);
    }

    public static ItemStack byItem(Item item) {
        if (mc.player == null) return null;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.isOf(item) && itemStack.getDamage() < 430) return itemStack;
        }
        return null;
    }

    public static boolean quickMoveFromTo(int from, int to) {
        if (from == -1 || to == -1 || mc.interactionManager == null || mc.player == null) return false;
        int syncId = getSyncId();
        mc.interactionManager.clickSlot(syncId, from, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, to, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, from, 0, SlotActionType.PICKUP, mc.player);
        return true;
    }

    public static int getSlotWithStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || mc.player == null) return -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack invStack = mc.player.getInventory().getStack(i);
            if (ItemStack.areEqual(invStack, stack)) return i;
        }
        return -1;
    }

    public interface Searcher {
        boolean isValid(ItemStack stack);
    }
}
