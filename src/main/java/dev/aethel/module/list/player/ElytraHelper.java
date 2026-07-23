package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import dev.aethel.event.list.EventPlayerUpdate;
import org.lwjgl.glfw.GLFW;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BindSetting;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.util.InventoryToolkit;
import dev.aethel.util.inventory.InventoryTask;
import dev.aethel.util.packet.NetworkUtils;
import dev.aethel.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "Elytra Helper", moduleCategory = ModuleCategory.PLAYER, moduleDesc = "Помощь при полёте на элитрах")
public class ElytraHelper extends Module {
    private final BindSetting swapKey = new BindSetting("Кнопка свапа", -1);
    private final BindSetting fireworkKey = new BindSetting("Кнопка феерверка", -1);
    private final BooleanSetting autoTakeoff = new BooleanSetting("Автовзлёт", false);

    private boolean swapPressed;
    private boolean fireworkPressed;
    private boolean fireworkUsed;
    private boolean swapped;

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (!swapped) return;
        swapped = false;
        bypassSwap(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA);
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null) return;

        // swap бинд как в src121: onTick с KeyStorage
        int swapKeyCode = swapKey.getValue();
        if (swapKeyCode != -1 && swapKeyCode != -999) {
            boolean swapNow = isBindPressed(swapKeyCode);
            if (swapNow && !swapPressed) {
                swapped = true;
            }
            swapPressed = swapNow;
        }

        // firework бинд как в src121: onTick с KeyStorage
        int fwKeyCode = fireworkKey.getValue();
        if (fwKeyCode != -1 && fwKeyCode != -999) {
            boolean fwNow = isBindPressed(fwKeyCode);
            if (fwNow && !fireworkPressed && mc.player.isGliding()) {
                fireworkUsed = true;
            }
            fireworkPressed = fwNow;
        }

        if (autoTakeoff.getValue()) {
            ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);

            if (chest.getItem() == Items.ELYTRA
                    && !mc.player.isInLava()
                    && !mc.player.isTouchingWater()
                    && mc.player.isOnGround()
                    && !mc.player.hasVehicle()
                    && !mc.player.isGliding()
                    && !mc.player.isSpectator()
                    && !mc.options.jumpKey.isPressed()) {
                mc.player.jump();
            }

            if (chest.getItem() == Items.ELYTRA
                    && !mc.player.isInLava()
                    && !mc.player.isTouchingWater()
                    && !mc.player.isOnGround()
                    && !mc.player.hasVehicle()
                    && !mc.player.isGliding()
                    && !mc.player.isSpectator()) {
                NetworkUtils.sendSilentPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                mc.player.startGliding();
            }
        }

        if (!fireworkUsed) return;
        fireworkUsed = false;

        InventoryUtil.useLegit(Items.FIREWORK_ROCKET);
    }

    private boolean isBindPressed(int keyCode) {
        if (keyCode == -1 || keyCode == -999) return false;
        if (keyCode >= 1 && keyCode <= 7) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private void bypassSwap(boolean chestplate) {
        int slot = chestplate ? InventoryUtil.findBestChestplateSlot() : InventoryUtil.findBestElytraSlot();
        if (slot == -1) return;

        if (slot >= 0 && slot <= 8) {
            InventoryUtil.swapBypass(() ->
                mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player)
            );
        } else if (slot >= 8 && slot <= mc.player.getInventory().main.size()) {
            InventoryUtil.swapBypass(() -> {
                mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, mc.player);
            });
        }
    }
}
