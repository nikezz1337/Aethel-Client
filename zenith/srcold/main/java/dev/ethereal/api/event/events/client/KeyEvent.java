package dev.ethereal.api.event.events.client;

import lombok.Getter;
import dev.ethereal.api.event.events.Event;

public class KeyEvent extends Event<KeyEvent.KeyEventData> {
    @Getter private static final KeyEvent instance = new KeyEvent();

    public record KeyEventData(int key, int action, boolean mouse) { }
}
