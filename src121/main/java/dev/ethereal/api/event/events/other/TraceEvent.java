package dev.ethereal.api.event.events.other;

import dev.ethereal.api.event.CancellableEvent;
import lombok.*;

@AllArgsConstructor @Getter @Setter
public class TraceEvent extends CancellableEvent {
    private float yaw, pitch;
}
