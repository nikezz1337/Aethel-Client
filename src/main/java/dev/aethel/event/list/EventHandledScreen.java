package dev.aethel.event.list;

import dev.aethel.event.Event;
import net.minecraft.screen.slot.Slot;

public class EventHandledScreen extends Event {
    private final Slot slotHover;

    public EventHandledScreen(Slot slotHover) {
        this.slotHover = slotHover;
    }

    public Slot getSlotHover() {
        return slotHover;
    }
}
