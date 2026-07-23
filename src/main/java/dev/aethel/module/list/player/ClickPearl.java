package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventKeyInput;
import dev.aethel.event.list.EventTick;
import dev.aethel.event.list.MoveInputEvent;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BindSetting;
import dev.aethel.util.InventoryToolkit;
import dev.aethel.util.inventory.InventoryTask;
import dev.aethel.util.keyboard.KeyStorage;
import dev.aethel.util.player.other.InventoryUtil;
import dev.aethel.util.world.ServerUtil;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.item.ItemStack;

@ModuleInformation(
        moduleName = "ClickPearl",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Бросок эндер-жемчуга по бинду"
)
public class ClickPearl extends Module {

    private final BindSetting throwKey = new BindSetting("Кнопка броска", -999);

    // ==================== ФАЗЫ ====================

    private enum Phase {
        READY,
        HOTBAR_USE,
        HOTBAR_RESTORE,
        INV_STOP,
        INV_WAIT,
        INV_SWAP_IN,
        INV_SWITCH,
        INV_USE,
        INV_SWAP_OUT,
        INV_CLOSE,
        FINISH
    }

    // ==================== СОСТОЯНИЕ ====================

    private Phase phase = Phase.READY;
    private int savedSlot = -1;
    private int itemSlot = -1;
    private int swapSlot = -1;
    private int hotbarSlot = -1;

    // ==================== БИНДЫ ====================

    @Subscribe
    public void onKey(EventKeyInput event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getAction() != 1) return;
        if (throwKey.getValue() == -999) return;
        if (!KeyStorage.isPressed(throwKey.getValue())) return;
        if (mc.currentScreen != null) return;
        if (phase != Phase.READY) return;

        if (ServerUtil.isHolyWorld() || ServerUtil.isSpookyTime()) {
            InventoryUtil.useLegit(Items.ENDER_PEARL);
            return;
        }

        startThrow();
    }

    // ==================== БЛОКИРОВКА ДВИЖЕНИЯ ====================

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (isInvPhase(phase)) {
            event.forward = 0;
            event.strafe = 0;
            event.jump = false;
            event.sneak = false;
        }
    }

    // ==================== ТИК (ФАЗЫ) ====================

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.interactionManager == null) {
            reset();
            return;
        }

        if (phase != Phase.READY) {
            execute();
        }
    }

    // ==================== СТАРТ ====================

    private void startThrow() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) return;

        savedSlot = mc.player.getInventory().selectedSlot;

        int[] slots = findPearl();
        int hb = slots[0];
        int inv = slots[1];

        if (hb == -1 && inv == -1) {
            reset();
            return;
        }

        if (hb != -1) {
            hotbarSlot = hb;
            InventoryToolkit.switchToLocal(hotbarSlot);
            phase = Phase.HOTBAR_USE;
        } else {
            itemSlot = inv;
            swapSlot = savedSlot;
            phase = Phase.INV_STOP;
        }
    }

    // ==================== ВЫПОЛНЕНИЕ ФАЗ ====================

    private void execute() {
        switch (phase) {
            case HOTBAR_USE -> {
                use();
                phase = Phase.HOTBAR_RESTORE;
            }
            case HOTBAR_RESTORE -> {
                InventoryToolkit.switchToLocal(savedSlot);
                phase = Phase.FINISH;
            }
            case INV_STOP -> phase = Phase.INV_WAIT;
            case INV_WAIT -> phase = Phase.INV_SWAP_IN;
            case INV_SWAP_IN -> {
                InventoryTask.clickSlot(itemSlot, swapSlot, SlotActionType.SWAP);
                phase = Phase.INV_SWITCH;
            }
            case INV_SWITCH -> {
                InventoryToolkit.switchToLocal(swapSlot);
                phase = Phase.INV_USE;
            }
            case INV_USE -> {
                use();
                phase = Phase.INV_SWAP_OUT;
            }
            case INV_SWAP_OUT -> {
                InventoryTask.clickSlot(itemSlot, swapSlot, SlotActionType.SWAP);
                phase = Phase.INV_CLOSE;
            }
            case INV_CLOSE -> {
                InventoryTask.closeScreen(true);
                phase = Phase.FINISH;
            }
            case FINISH -> reset();
            default -> {}
        }
    }

    // ==================== УТИЛИТЫ ====================

    private void use() {
        InventoryToolkit.interactItem(Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int[] findPearl() {
        int hotbar = -1;
        int container = -1;
        DefaultedList<ItemStack> main = mc.player.getInventory().main;
        for (int i = 0; i < main.size(); i++) {
            ItemStack stack = main.get(i);
            if (stack.isEmpty() || !stack.isOf(Items.ENDER_PEARL)) continue;
            if (i < 9) {
                if (hotbar == -1) hotbar = i;
                if (container == -1) container = i + 36;
            } else if (container == -1) {
                container = i;
            }
            if (hotbar != -1 && container != -1) break;
        }
        return new int[]{hotbar, container};
    }

    private boolean isInvPhase(Phase p) {
        return switch (p) {
            case INV_STOP, INV_WAIT, INV_SWAP_IN, INV_SWITCH, INV_USE, INV_SWAP_OUT, INV_CLOSE -> true;
            default -> false;
        };
    }

    private void reset() {
        if (mc.player != null && savedSlot >= 0 && savedSlot <= 8) {
            InventoryToolkit.switchToLocal(savedSlot);
        }
        phase = Phase.READY;
        savedSlot = -1;
        itemSlot = -1;
        swapSlot = -1;
        hotbarSlot = -1;
    }

    @Override
    public void onDisable() {
        reset();
    }
}
