package dev.aethel.util.inventory.group;

import dev.aethel.util.inventory.group.impl.ArmorSlotsGroup;
import dev.aethel.util.inventory.group.impl.HotbarSlotsGroup;
import dev.aethel.util.inventory.group.impl.InventorySlotsGroup;
import dev.aethel.util.inventory.group.impl.OffhandSlotGroup;
import dev.aethel.util.inventory.slots.ArmorSlot;
import dev.aethel.util.inventory.slots.HotbarSlot;
import dev.aethel.util.inventory.slots.InventorySlot;
import dev.aethel.util.inventory.slots.OffhandSlot;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> hotbar() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> inventory() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> armor() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> offhand() {
      return new OffhandSlotGroup();
   }
}
