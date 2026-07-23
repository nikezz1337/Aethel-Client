package dev.ethereal.api.utils.player;

import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.inject.accessors.HandledScreenAccessor;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

public final class InventoryActionUtil implements QuickImports {
    public static final int OFFHAND_SLOT = 45;
    public static final int CHEST_ARMOR_SLOT = 6;

    private InventoryActionUtil() {
    }

    public static boolean matchesBind(KeyEvent event, int bind) {
        if (bind == -1 || bind == -999) return false;
        if (event.mouse()) {
            return bind == -100 + event.key() || bind == event.key();
        }
        return bind == event.key();
    }

    public static int toScreenSlot(int inventorySlot) {
        if (inventorySlot < 0) return -1;
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }

    public static int toInventorySlot(int screenSlot) {
        if (screenSlot >= 36 && screenSlot <= 44) return screenSlot - 36;
        return screenSlot;
    }

    public static int findHotbarSlot(Item item) {
        if (mc.player == null) return -1;
        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        for (int i = 0; i < 9; i++) {
            if (main.get(i).isOf(item)) return i;
        }
        return -1;
    }

    public static int findScreenSlot(Item item) {
        return findScreenSlot(stack -> stack.isOf(item), true);
    }

    public static int findScreenSlot(Item item, boolean includeHotbar) {
        return findScreenSlot(stack -> stack.isOf(item), includeHotbar);
    }

    public static int findScreenSlot(Predicate<ItemStack> predicate, boolean includeHotbar) {
        if (mc.player == null) return -1;
        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        int first = includeHotbar ? 0 : 9;
        for (int i = first; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (!stack.isEmpty() && predicate.test(stack)) return toScreenSlot(i);
        }
        return -1;
    }

    public static int findBestHotbarSlot() {
        int slot = InventoryUtil.findBestSlotInHotBar();
        return slot == -1 && mc.player != null ? mc.player.getInventory().selectedSlot : slot;
    }

    public static void clickSlot(int slot, int button, SlotActionType actionType) {
        if (slot < 0 || mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, actionType, mc.player);
    }

    public static void swapWithHotbar(int screenSlot, int hotbarSlot) {
        clickSlot(screenSlot, hotbarSlot, SlotActionType.SWAP);
    }

    public static void swapToOffhand(int screenSlot) {
        clickSlot(screenSlot, 40, SlotActionType.SWAP);
    }

    public static void closeHandledScreenPacket() {
        if (mc.player == null) return;
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    public static void switchTo(int hotbarSlot) {
        InventoryUtil.swapToSlot(hotbarSlot);
    }

    public static void useMainHand() {
        InventoryUtil.useItem(Hand.MAIN_HAND);
    }

    public static void useOffHand() {
        InventoryUtil.useItem(Hand.OFF_HAND);
    }

    public static MovementSnapshot stopMovement() {
        MovementSnapshot snapshot = MovementSnapshot.capture();
        if (mc.player != null) {
            mc.player.input.movementForward = 0;
            mc.player.input.movementSideways = 0;
            mc.player.setSprinting(false);
        }
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        return snapshot;
    }

    public static void moveCursorToSlot(HandledScreen<?> screen, int slotIndex) {
        Slot slot = getSlot(screen, slotIndex);
        if (slot == null) return;

        int guiX = ((HandledScreenAccessor) screen).ethereal$getX() + slot.x + 8;
        int guiY = ((HandledScreenAccessor) screen).ethereal$getY() + slot.y + 8;
        double scale = mc.getWindow().getScaleFactor();

        GLFW.glfwSetCursorPos(mc.getWindow().getHandle(), guiX * scale, guiY * scale);
        screen.mouseMoved(guiX, guiY);
    }

    public static void clickScreenSlot(HandledScreen<?> screen, int slotIndex, int button, SlotActionType actionType) {
        Slot slot = getSlot(screen, slotIndex);
        if (slot == null) return;
        ((HandledScreenAccessor) screen).ethereal$onMouseClick(slot, slot.id, button, actionType);
    }

    public static Slot getSlot(HandledScreen<?> screen, int slotIndex) {
        if (screen == null) return null;
        if (slotIndex < 0 || slotIndex >= screen.getScreenHandler().slots.size()) return null;
        return screen.getScreenHandler().slots.get(slotIndex);
    }

    public static void closeCurrentScreenWithInventoryKey() {
        if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
    }

    public static void pressHotbarKey(HandledScreen<?> screen, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return;
        screen.keyPressed(GLFW.GLFW_KEY_1 + hotbarSlot, 0, 0);
    }

    public static final class MovementSnapshot {
        private final boolean forward;
        private final boolean back;
        private final boolean left;
        private final boolean right;
        private final boolean jump;
        private final boolean sneak;
        private final boolean sprint;

        private MovementSnapshot(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
            this.forward = forward;
            this.back = back;
            this.left = left;
            this.right = right;
            this.jump = jump;
            this.sneak = sneak;
            this.sprint = sprint;
        }

        public static MovementSnapshot capture() {
            return new MovementSnapshot(
                    isPhysicalPressed(mc.options.forwardKey),
                    isPhysicalPressed(mc.options.backKey),
                    isPhysicalPressed(mc.options.leftKey),
                    isPhysicalPressed(mc.options.rightKey),
                    isPhysicalPressed(mc.options.jumpKey),
                    isPhysicalPressed(mc.options.sneakKey),
                    isPhysicalPressed(mc.options.sprintKey)
            );
        }

        public void restore() {
            mc.options.forwardKey.setPressed(forward && isPhysicalPressed(mc.options.forwardKey));
            mc.options.backKey.setPressed(back && isPhysicalPressed(mc.options.backKey));
            mc.options.leftKey.setPressed(left && isPhysicalPressed(mc.options.leftKey));
            mc.options.rightKey.setPressed(right && isPhysicalPressed(mc.options.rightKey));
            mc.options.jumpKey.setPressed(jump && isPhysicalPressed(mc.options.jumpKey));
            mc.options.sneakKey.setPressed(sneak && isPhysicalPressed(mc.options.sneakKey));
            mc.options.sprintKey.setPressed(sprint && isPhysicalPressed(mc.options.sprintKey));

            if (mc.player != null) {
                if (forward && isPhysicalPressed(mc.options.forwardKey)) mc.player.input.movementForward = 1.0f;
                if (back && isPhysicalPressed(mc.options.backKey)) mc.player.input.movementForward = -1.0f;
                if (left && isPhysicalPressed(mc.options.leftKey)) mc.player.input.movementSideways = 1.0f;
                if (right && isPhysicalPressed(mc.options.rightKey)) mc.player.input.movementSideways = -1.0f;
            }
        }

        private static boolean isPhysicalPressed(KeyBinding key) {
            return InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.getDefaultKey().getCode());
        }
    }
}
