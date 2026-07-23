package dev.ethereal.api.event.events.player.world;

import dev.ethereal.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class PushEvent extends CancellableEvent {
    private final PushingSource source;

    public enum PushingSource {
        BLOCK, WATER, ENTITY
    }
}
