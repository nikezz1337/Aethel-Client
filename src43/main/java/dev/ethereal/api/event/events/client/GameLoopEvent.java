package dev.ethereal.api.event.events.client;

import lombok.Getter;
import dev.ethereal.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
    @Getter private static final GameLoopEvent instance = new GameLoopEvent();
}
