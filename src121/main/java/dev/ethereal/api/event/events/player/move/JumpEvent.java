package dev.ethereal.api.event.events.player.move;

import dev.ethereal.api.event.CancellableEvent;
import lombok.*;

@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class JumpEvent extends CancellableEvent {
    private float yaw;
}
