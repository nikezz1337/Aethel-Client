package dev.aethel.util;

import com.google.common.eventbus.Subscribe;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.list.combat.aura.util.time.TimerUtil;
import dev.aethel.util.inventory.InventoryUtility;
import dev.aethel.util.inventory.ItemSlot;
import dev.aethel.util.inventory.group.SlotGroup;
import dev.aethel.util.inventory.group.SlotGroups;
import dev.aethel.util.inventory.slots.HotbarSlot;
import dev.aethel.util.inventory.slots.InventorySlot;
import dev.aethel.util.player.other.SlownessManager;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class SwapIntegration implements IMinecraft, QuickLogger {
   private Item itemToUse = null;
   private HotbarSlot originalSlot = null;
   private boolean isProcessingItem = false;
   private boolean slowTaskPending = false;
   private ItemSlot targetSlot = null;
   private final TimerUtil itemUseTimer = new TimerUtil();
   private ItemUseState currentState = ItemUseState.IDLE;

   public SwapIntegration() {
      Aethel.getInstance().getEventBus().register(this);
   }

   @Subscribe
   private void onTick(EventTick event) {
      if (this.isProcessingItem) {
         this.processItemUse();
      }
   }

   private void processItemUse() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.player.getItemCooldownManager() == null) {
         this.resetUseState();
         return;
      }

      switch (this.currentState) {
         case USING_ITEM:
            if (this.slowTaskPending) break;
            this.slowTaskPending = true;
            SlownessManager.addTask(new SlownessManager.SlowTask(50, 0, () -> {
               mc.interactionManager
                  .sendSequencedPacket(
                     mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch())
                  );
               this.currentState = ItemUseState.RETURNING_SLOT;
            }));
            break;
         case RETURNING_SLOT:
            if (this.targetSlot instanceof HotbarSlot) {
               InventoryUtility.selectHotbarSlot(this.originalSlot);
               this.resetUseState();
            } else if (this.targetSlot instanceof InventorySlot itemInventorySlot) {
               InventoryUtility.hotbarSwap(itemInventorySlot.getIdForServer(), this.originalSlot.getSlotId());
               this.resetUseState();
            }
            break;
         default:
            this.resetUseState();
      }
   }

   public void useItem(Item itemType) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
         return;
      }
      if (this.isProcessingItem) {
         return;
      }

      SlotGroup<ItemSlot> group = SlotGroups.hotbar().and(SlotGroups.inventory());
      ItemSlot itemSlot = group.findItem(itemType);

      if (itemSlot == null) {
         this.logChat("Предмет не найден — необходимо иметь " + itemType.getName().getString() + " в инвентаре");
         return;
      }

      if (mc.player.getItemCooldownManager().isCoolingDown(itemSlot.itemStack())) {
         return;
      }

      this.itemToUse = itemType;
      this.originalSlot = InventoryUtility.getCurrentHotbarSlot();
      this.targetSlot = itemSlot;
      this.isProcessingItem = true;
      this.currentState = ItemUseState.USING_ITEM;
      this.itemUseTimer.reset();

      if (itemSlot instanceof HotbarSlot itemHotbarSlot) {
         if (InventoryUtility.getCurrentHotbarSlot().item() != itemType) {
            InventoryUtility.selectHotbarSlot(itemHotbarSlot);
         }
      } else if (itemSlot instanceof InventorySlot itemInventorySlot) {
         HotbarSlot currentSlot = InventoryUtility.getCurrentHotbarSlot();
         InventoryUtility.hotbarSwap(itemInventorySlot.getIdForServer(), currentSlot.getSlotId());
      }
   }

   private void resetUseState() {
      this.isProcessingItem = false;
      this.slowTaskPending = false;
      this.currentState = ItemUseState.IDLE;
      this.itemToUse = null;
      this.originalSlot = null;
      this.targetSlot = null;
   }

   private enum ItemUseState {
      IDLE,
      USING_ITEM,
      RETURNING_SLOT
   }
}
