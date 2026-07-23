package dev.ethereal.api.event.events.player.other;

import lombok.Getter;
import dev.ethereal.api.event.events.Event;

public class CloseScreenEvent extends Event<CloseScreenEvent> {
    @Getter private static final CloseScreenEvent instance = new CloseScreenEvent();
}
