package antileak.base.api.events.implement;

import lombok.AllArgsConstructor;
import antileak.base.api.events.Event;

@AllArgsConstructor
public class EventCloseInv extends Event {
    public int windowId;
}

