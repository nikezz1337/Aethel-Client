package dev.ethereal.api.event.events.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;

@Getter
@AllArgsConstructor
public class VelocityEvent {
    @Setter private Vec3d movementInput;
    private float speed;
    private float yaw;

    @Setter private Vec3d velocity;
}
