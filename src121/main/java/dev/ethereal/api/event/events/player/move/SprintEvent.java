package dev.ethereal.api.event.events.player.move;

import dev.ethereal.api.utils.player.DirectionalInput;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class SprintEvent {
    private boolean sprint = false;

    private final DirectionalInput directionalInput;
}
