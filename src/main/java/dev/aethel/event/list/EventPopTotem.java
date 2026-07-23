package dev.aethel.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import dev.aethel.event.Event;

@Getter
@AllArgsConstructor
public class EventPopTotem extends Event {
    private final PlayerEntity player;
}
