package dev.ethereal.api.event.events.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class KeyEvent {
    private final int key;
    private final int action;
    private final boolean mouse;
}
