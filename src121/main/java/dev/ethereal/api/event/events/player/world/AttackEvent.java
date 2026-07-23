package dev.ethereal.api.event.events.player.world;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class AttackEvent {
    private final Entity entity;
}
