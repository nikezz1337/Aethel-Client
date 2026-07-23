package dev.ethereal.api.utils.player;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.inject.accessors.ClientPlayerInteractionManagerAccessor;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class InventoryTask implements QuickImports {
    private InventoryTask() {
    }

    private static int getSyncId() {
        if (mc.player == null) return 0;
        if (mc.player.currentScreenHandler != null) return mc.player.currentScreenHandler.syncId;
        if (mc.player.playerScreenHandler != null) return mc.player.playerScreenHandler.syncId;
        return 0;
    }

    public static void moveItem(Slot from, int to) {
        if (from != null) moveItem(from.id, to, false, false);
    }

    public static void moveItem(Slot from, int to, boolean task) {
        moveItem(from, to, task, false);
    }

    public static void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
        if (from != null) moveItem(from.id, to, task, updateInventory);
    }

    public static void moveItem(int from, int to, boolean task, boolean updateInventory) {
        if (from == to || from == -1) return;
        if (task) InventoryFlowManager.addTask(() -> moveItem(from, to, updateInventory));
        else moveItem(from, to, updateInventory);
    }

    public static void moveItem(int from, int to, boolean updateInventory) {
        int count = Math.toIntExact(slots().count()) - 10;
        if (count == 36) {
            if (to == 45 && from >= 0 && from <= 44) {
                clickSlot(from, 40, SlotActionType.SWAP, false);
                if (updateInventory) updateSlots();
                return;
            }
            if (from == 45 && to >= 0 && to <= 44) {
                clickSlot(to, 40, SlotActionType.SWAP, false);
                if (updateInventory) updateSlots();
                return;
            }

            int hotbarStart = count;
            int hotbarEnd = count + 8;
            if (from >= hotbarStart && from <= hotbarEnd) {
                clickSlot(to, from - hotbarStart, SlotActionType.SWAP, false);
                if (updateInventory) updateSlots();
                return;
            }
            if (to >= hotbarStart && to <= hotbarEnd) {
                clickSlot(from, to - hotbarStart, SlotActionType.SWAP, false);
                if (updateInventory) updateSlots();
                return;
            }
        }

        clickSlot(from, 0, SlotActionType.PICKUP, false);
        clickSlot(to, 0, SlotActionType.PICKUP, false);
        clickSlot(from, 0, SlotActionType.PICKUP, false);
        if (updateInventory) updateSlots();
    }

    public static void swapHand(Slot slot, Hand hand, boolean task) {
        swapHand(slot, hand, task, false);
    }

    public static void swapHand(Slot slot, Hand hand, boolean task, boolean updateInventory) {
        if (slot == null || slot.id == -1
                || (hand == Hand.OFF_HAND && !(slot.inventory instanceof PlayerInventory || slot.inventory instanceof EnderChestInventory))) {
            return;
        }
        int button = hand == Hand.MAIN_HAND ? mc.player.getInventory().selectedSlot : 40;
        if (task) InventoryFlowManager.addTask(() -> swap(slot, button, updateInventory));
        else swap(slot, button, updateInventory);
    }

    public static void swap(Slot slot, int button, boolean updateInventory) {
        clickSlot(slot, button, SlotActionType.SWAP, false);
        if (updateInventory) updateSlots();
    }

    public static void swapAndUse(Slot slot) {
        swapHand(slot, Hand.MAIN_HAND, false);
        InventoryToolkit.sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND, i, mc.player.getYaw(), mc.player.getPitch()));
        swapHand(slot, Hand.MAIN_HAND, false, true);
    }

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id < 0 || type == null || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(getSyncId(), id, button, type, mc.player);
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

    public static void updateSlots() {
        if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) return;
        int selectedSlot = mc.player.getInventory().selectedSlot;
        if (mc.interactionManager instanceof ClientPlayerInteractionManagerAccessor accessor) {
            accessor.ethereal$syncSelectedSlot();
        } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
        }
    }

    public static void closeScreen(boolean packet) {
        if (mc.player == null) return;
        boolean hasContainerOpen = mc.player.currentScreenHandler != null
                && mc.player.playerScreenHandler != null
                && mc.player.currentScreenHandler.syncId != mc.player.playerScreenHandler.syncId;
        if (!hasContainerOpen) return;
        if (packet) {
            if (mc.player.networkHandler != null) {
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(getSyncId()));
            }
        } else {
            mc.player.closeHandledScreen();
        }
    }

    public static void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
        if (slot != null) clickSlot(slot.id, button, clickType, silent);
    }

    public static void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        clickSlot(getSyncId(), slotId, buttonId, clickType, silent);
    }

    public static void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        if (slotId < 0 || clickType == null || mc.interactionManager == null || mc.player == null) return;
        int syncId = windowId <= 0 ? getSyncId() : windowId;
        mc.interactionManager.clickSlot(syncId, slotId, buttonId, clickType, mc.player);
        if (silent && mc.player.currentScreenHandler != null) {
            mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, mc.player);
        }
    }

    public static Slot getSlot(Item item) {
        return getSlot(item, slot -> true);
    }

    public static Slot getSlot(Item item, Predicate<Slot> filter) {
        return getSlot(item, Comparator.comparingInt(slot -> 0), filter);
    }

    public static Slot getSlot(Predicate<Slot> filter) {
        return slots().filter(filter).findFirst().orElse(null);
    }

    public static Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
        return slots().filter(filter).max(comparator).orElse(null);
    }

    public static Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
        return slots().filter(slot -> slot.getStack().isOf(item)).filter(filter).max(comparator).orElse(null);
    }

    public static Slot getSlot(List<Item> items) {
        return slots().filter(slot -> items.contains(slot.getStack().getItem())).findFirst().orElse(null);
    }

    public static int getInventoryCount(Item item) {
        if (mc.player == null) return 0;
        return IntStream.range(0, 45)
                .filter(i -> Objects.requireNonNull(mc.player).getInventory().getStack(i).isOf(item))
                .map(i -> mc.player.getInventory().getStack(i).getCount())
                .sum();
    }

    public static int getHotbarItems(List<Item> items) {
        if (mc.player == null) return -1;
        return IntStream.range(0, 9)
                .filter(i -> items.contains(mc.player.getInventory().getStack(i).getItem()))
                .findFirst()
                .orElse(-1);
    }

    public static int getHotbarSlotId(IntPredicate filter) {
        return IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
    }

    public static int getCount(Predicate<Slot> filter) {
        return slots().filter(filter).mapToInt(slot -> slot.getStack().getCount()).sum();
    }

    public static Slot mainHandSlot() {
        long count = slots().count();
        int i = count == 46 ? 10 : 9;
        return slots().toList().get(Math.toIntExact(count - i + mc.player.getInventory().selectedSlot));
    }

    public static boolean isServerScreen() {
        return slots().toList().size() != 46;
    }

    public static Stream<Slot> slots() {
        if (mc.player == null || mc.player.currentScreenHandler == null) return Stream.empty();
        return mc.player.currentScreenHandler.slots.stream();
    }

    public static float getCooldownProgress(Item item) {
        if (mc.player == null) return 0.0f;
        ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
        if (cooldownManager == null) return 0.0f;
        return cooldownManager.getCooldownProgress(item.getDefaultStack(), mc.getRenderTickCounter().getTickDelta(true));
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        InventoryToolkit.sendSequencedPacket(packetCreator);
    }

    public static String getCleanName(Text text) {
        if (text == null) return "";
        String name = text.getString();
        if (name == null) return "";
        return name.replaceAll("§[0-9a-fk-or]", "").toLowerCase();
    }
}
