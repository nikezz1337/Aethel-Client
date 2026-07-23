package dev.ethereal.client.features.modules.player;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.other.SlownessManager;

@ModuleRegister(name = "Auto Tool", category = Category.PLAYER)
public class AutoToolModule extends Module {
    @Getter private static final AutoToolModule instance = new AutoToolModule();

    private int originalSlot = -1;
    private int swappedFromInv = -1;
    private boolean isSwapping = false;

    @EventHandler
    public void onUpdate(UpdateEvent event) {
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
            if (swappedFromInv != -1) {
                isSwapping = true;
                SlownessManager.applySlowness(10L, () -> {
                    InventoryActionUtil.clickSlot(swappedFromInv, originalSlot, SlotActionType.SWAP);
                    swappedFromInv = -1;
                    isSwapping = false;
                });
                return;
            }
            InventoryUtil.swapToSlot(bestSlot);
        } else {
            if (swappedFromInv == bestSlot) return;

            if (swappedFromInv != -1) {
                isSwapping = true;
                SlownessManager.applySlowness(10L, () -> {
                    InventoryActionUtil.clickSlot(swappedFromInv, originalSlot, SlotActionType.SWAP);
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
                InventoryActionUtil.clickSlot(finalBestSlot, currentSlot, SlotActionType.SWAP);
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
                InventoryActionUtil.clickSlot(swappedFromInv, originalSlot, SlotActionType.SWAP);
                swappedFromInv = -1;
                isSwapping = false;
            });
            return;
        }

        if (originalSlot != -1 && mc.player.getInventory().selectedSlot != originalSlot) {
            InventoryUtil.swapToSlot(originalSlot);
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