package dev.ethereal.api.event.events.player.world;

import dev.ethereal.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.screen.slot.SlotActionType;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ClickSlotEvent extends CancellableEvent {
    private final SlotActionType slotActionType;
    private final int slot;
    private final int button;
    private final int id;
}
