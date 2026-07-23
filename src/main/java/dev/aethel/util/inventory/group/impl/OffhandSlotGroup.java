package dev.aethel.util.inventory.group.impl;

import dev.aethel.util.inventory.group.SlotGroup;
import dev.aethel.util.inventory.slots.OffhandSlot;

import java.util.List;

public class OffhandSlotGroup extends SlotGroup<OffhandSlot> {
   public OffhandSlotGroup() {
      super(List.of(new OffhandSlot()));
   }
}
