package dev.aethel.event.list;

import dev.aethel.event.Event;
import net.minecraft.screen.slot.SlotActionType;

public class EventClickSlot extends Event {
    private final int slotId;
    private final int button;
    private final SlotActionType actionType;

    public EventClickSlot(int slotId, int button, SlotActionType actionType) {
        this.slotId = slotId;
        this.button = button;
        this.actionType = actionType;
    }

    public int getSlotId() { return slotId; }
    public int getButton() { return button; }
    public SlotActionType getActionType() { return actionType; }
}
