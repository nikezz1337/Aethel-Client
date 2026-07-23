package dev.ethereal.api.event.events.player.move;

import dev.ethereal.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class TravelEvent extends CancellableEvent {
    private float yaw, pitch;
}
