package dev.aethel.event.list;

import dev.aethel.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.hit.BlockHitResult;

@Getter
@AllArgsConstructor
public class EventInteractBlock extends Event {
    private final BlockHitResult hitResult;
}
