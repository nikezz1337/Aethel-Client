package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.util.inventory.InventoryTask;
import dev.aethel.util.SlownessManager;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;

@ModuleInformation(
        moduleName = "AutoTool",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Автоматическая смена лучшего инструмента"
)
public class AutoTool extends Module {

    private int originalSlot = -1;
    private int swappedFromInv = -1;
    private boolean isSwapping = false;

    @Subscribe
    public void onUpdate(EventPlayerUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.isCreative()) {
            reset();
            return;
        }

        boolean breaking = mc.interactionManager.isBreakingBlock();

        if (breaking) {
            if (originalSlot == -1) {
                originalSlot = mc.player.getInventory().selectedSlot;
            }
            applyBestTool();
        } else {
            restoreTool();
        }
    }

    private void applyBestTool() {
        if (isSwapping) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        Block block = mc.world.getBlockState(hit.getBlockPos()).getBlock();

        int bestSlot = -1;
        float bestSpeed = 1.0f;

        // Ищем во всём инвентаре (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(block.getDefaultState());
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot == -1) return;
        if (bestSlot == mc.player.getInventory().selectedSlot) return;

        if (bestSlot < 9) {
            // Лучший тул в хотбаре — просто свапаем
            if (swappedFromInv != -1) {
                // Сначала вернуть инвентарный свап
                isSwapping = true;
                SlownessManager.applySlowness(10L, () -> {
                    InventoryTask.clickSlot(swappedFromInv, originalSlot, SlotActionType.SWAP);
                    swappedFromInv = -1;
                    isSwapping = false;
                });
                return;
            }
            mc.player.getInventory().selectedSlot = bestSlot;
        } else {
            // Лучший тул в инвентаре — свапаем через слот
            if (swappedFromInv == bestSlot) return;

            if (swappedFromInv != -1) {
                // Уже свапали из инвентаря — вернуть, потом поменять
                isSwapping = true;
                SlownessManager.applySlowness(10L, () -> {
                    InventoryTask.clickSlot(swappedFromInv, originalSlot, SlotActionType.SWAP);
                    swappedFromInv = -1;
                    isSwapping = false;
                    applyBestTool();
                });
                return;
            }

            int currentSlot = mc.player.getInventory().selectedSlot;
            isSwapping = true;
            int finalBestSlot = bestSlot;
            SlownessManager.applySlowness(10L, () -> {
                InventoryTask.clickSlot(finalBestSlot, currentSlot, SlotActionType.SWAP);
                swappedFromInv = finalBestSlot;
                isSwapping = false;
            });
        }
    }

    private void restoreTool() {
        if (isSwapping) return;

        if (swappedFromInv != -1 && originalSlot != -1) {
            isSwapping = true;
            SlownessManager.applySlowness(10L, () -> {
                InventoryTask.clickSlot(swappedFromInv, originalSlot, SlotActionType.SWAP);
                swappedFromInv = -1;
                isSwapping = false;
            });
            return;
        }

        if (originalSlot != -1 && mc.player.getInventory().selectedSlot != originalSlot) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        originalSlot = -1;
    }

    private void reset() {
        originalSlot = -1;
        swappedFromInv = -1;
        isSwapping = false;
    }

    @Override
    public void onDisable() {
        restoreTool();
        reset();
    }
}
