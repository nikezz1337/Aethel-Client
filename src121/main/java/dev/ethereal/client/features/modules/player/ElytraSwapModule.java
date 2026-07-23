package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.features.modules.movement.InventoryMoveModule;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.system.backend.KeyStorage;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.other.SlownessManager;

@ModuleRegister(name = "Elytra Swap", category = Category.PLAYER)
public class ElytraSwapModule extends Module {
    @Getter private static final ElytraSwapModule instance = new ElytraSwapModule();

    private final BindSetting swapKey = new BindSetting("Бинд свапа").value(-999);

    private final BindSetting launchKey = new BindSetting("Бинд фейерверка").value(-999);

    private final BooleanSetting fly = new BooleanSetting("/fly на элитре").value(false);

    private final InventoryUtil.ItemUsage itemUsage = new InventoryUtil.ItemUsage(Items.FIREWORK_ROCKET, this);
    private boolean swapUsed = false;
    boolean packet;

    public ElytraSwapModule() {
        addSettings(swapKey, launchKey, fly);
        itemUsage.setUseRotation(false);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        handleMainLogic(!SlownessManager.isEnabled());
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        packet = PlayerUtil.isHW();
        handleMainLogic(SlownessManager.isEnabled());
    }

    private void handleMainLogic(boolean slow) {
        handleFireworkLaunch(slow);
        handleChestplateSwap(slow);
    }

    public void handleFireworkLaunch(boolean tick) {
        if (tick || !mc.player.isGliding()) return;

        itemUsage.handleUse(launchKey.getValue(), !packet && InventoryMoveModule.getInstance().isLegit());
    }

    public void handleChestplateSwap(boolean tick) {
        if (tick) return;

        if (KeyStorage.isPressed(swapKey.getValue())) {
            if (!swapUsed && mc.currentScreen == null) {
                if (slots() == -1) return;

                long delay = PlayerUtil.isST() ? 150L : 30L;
                SlownessManager.applySlowness(delay, () -> {
                    swapChestplate();
                    swapUsed = true;
                });
            }
        } else {
            swapUsed = false;
        }
    }

    public void swapChestplate() {
        if (mc.player == null || mc.interactionManager == null) return;

        if (InventoryUtil.hasElytraEquipped()) {
            int slot = slots();

            if (slot != -1) {
                if (slot >= 0 && slot <= 8) {
                    InventoryUtil.swapSlotsFull(6, slot);
                } else if (slot >= 36 && slot <= 44) {
                    int hotbarSlot = slot - 36;
                    InventoryUtil.swapSlotsFull(6, hotbarSlot);
                } else {
                    int emptySlot = InventoryUtil.findEmptySlot();
                    if (emptySlot == -1) {
                        emptySlot = InventoryUtil.findBestSlotInHotBar();
                    }

                    if (emptySlot != -1) {
                        InventoryUtil.swapSlots(slot, emptySlot);
                        InventoryUtil.swapSlotsFull(6, emptySlot);
                        InventoryUtil.swapSlots(slot, emptySlot);
                    }
                }
            }
        } else {
            int slot = slots();

            if (slot != -1) {
                if (slot >= 0 && slot <= 8) {
                    InventoryUtil.swapSlotsFull(6, slot);
                } else if (slot >= 36 && slot <= 44) {
                    int hotbarSlot = slot - 36;
                    InventoryUtil.swapSlotsFull(6, hotbarSlot);
                } else {
                    int emptySlot = InventoryUtil.findEmptySlot();
                    if (emptySlot == -1) {
                        emptySlot = InventoryUtil.findBestSlotInHotBar();
                    }

                    if (emptySlot != -1) {
                        InventoryUtil.swapSlots(slot, emptySlot);
                        InventoryUtil.swapSlotsFull(6, emptySlot);
                        InventoryUtil.swapSlots(slot, emptySlot);
                    }
                }
                if (fly.getValue()) {
                    mc.player.networkHandler.sendChatCommand("fly");
                }
            }
        }
    }

    private int findBestSlotFor(Item... items) {
        for (Item item : items) {
            int slot = InventoryUtil.findItem(item);
            if (slot != -1) return slot;
        }
        return -1;
    }

    public int slots() {
        return InventoryUtil.hasElytraEquipped() ? findChestplateSlot() : findElytraSlot();
    }

    public int findElytraSlot() {
        return findBestSlotFor(Items.ELYTRA);
    }

    public int findChestplateSlot() {
        return findBestSlotFor(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE);
    }
}
