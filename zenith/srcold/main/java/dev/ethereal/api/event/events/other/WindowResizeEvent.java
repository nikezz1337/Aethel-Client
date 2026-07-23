package dev.ethereal.api.event.events.other;

import lombok.Getter;
import dev.ethereal.api.event.events.Event;

public class WindowResizeEvent extends Event<WindowResizeEvent> {
    @Getter private static final WindowResizeEvent instance = new WindowResizeEvent();
}
