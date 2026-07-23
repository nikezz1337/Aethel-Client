package dev.ethereal.client.features.modules.player;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Tool", category = Category.PLAYER)
public class AutoToolModule extends Module {
    @Getter private static final AutoToolModule instance = new AutoToolModule();

    private int lastSlot = -1;
    private int swappedFromInv = -1; // inventory slot that was swapped into hotbar

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.isCreative()) {
                reset();
                return;
            }

            boolean breaking = mc.interactionManager.isBreakingBlock();

            if (breaking) {
                if (lastSlot == -1) lastSlot = mc.player.getInventory().selectedSlot;
                applyBestTool();
            } else {
                restoreTool();
            }
        }));

        addEvents(updateEvent);
    }

    private void applyBestTool() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
        Block block = mc.world.getBlockState(hit.getBlockPos()).getBlock();

        int bestSlot = -1;
        float bestSpeed = 1.0f;

        // Search all inventory slots (0-35)
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

        // If best tool is in hotbar (0-8), just select it
        if (bestSlot < 9) {
            mc.player.getInventory().selectedSlot = bestSlot;
            swappedFromInv = -1;
        } else {
            // Tool is in inventory (9-35) — swap with current hotbar slot
            if (swappedFromInv != bestSlot) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        bestSlot,
                        lastSlot,
                        SlotActionType.SWAP,
                        mc.player
                );
                swappedFromInv = bestSlot;
            }
            mc.player.getInventory().selectedSlot = lastSlot;
        }
    }

    private void restoreTool() {
        // Swap back if we swapped from inventory
        if (swappedFromInv != -1 && lastSlot != -1) {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    swappedFromInv,
                    lastSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
            swappedFromInv = -1;
        }

        // Restore original slot
        if (lastSlot != -1) {
            mc.player.getInventory().selectedSlot = lastSlot;
            lastSlot = -1;
        }
    }

    private void reset() {
        lastSlot = -1;
        swappedFromInv = -1;
    }

    @Override
    public void onDisable() {
        restoreTool();
        reset();
    }
}
