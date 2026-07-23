package dev.ethereal.api.event.events.render;

import dev.ethereal.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
public class EntityColorEvent extends CancellableEvent {
    private int color;
}
