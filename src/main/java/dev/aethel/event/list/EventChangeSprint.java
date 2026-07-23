package dev.aethel.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import dev.aethel.event.Event;

@Getter
@Setter
@AllArgsConstructor
public class EventChangeSprint extends Event {
    private boolean sprinting;
}
