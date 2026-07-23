package dev.aethel.util.inventory.group.impl;

import dev.aethel.util.inventory.group.SlotGroup;
import dev.aethel.util.inventory.slots.ArmorSlot;

import java.util.ArrayList;
import java.util.List;

public class ArmorSlotsGroup extends SlotGroup<ArmorSlot> {
   public ArmorSlotsGroup() {
      super(createSlots());
   }

   private static List<ArmorSlot> createSlots() {
      List<ArmorSlot> slots = new ArrayList<>();

      for (int i = 0; i < 4; i++) {
         slots.add(new ArmorSlot(i));
      }

      return slots;
   }
}
