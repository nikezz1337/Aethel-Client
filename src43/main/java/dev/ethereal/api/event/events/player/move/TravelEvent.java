package dev.ethereal.api.event.events.player.move;

import lombok.Getter;
import dev.ethereal.api.event.events.Event;

public class TravelEvent extends Event<TravelEvent> {
    @Getter private static final TravelEvent instance = new TravelEvent();
}
