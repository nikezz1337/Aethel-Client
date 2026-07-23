package dev.ethereal.api.event.events.player.move;

import dev.ethereal.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;

@Getter
@Setter
@AllArgsConstructor
public class MoveEvent extends CancellableEvent {
    private double x;
    private double y;
    private double z;

    public void set(Vec3d vec3d) {
        x = vec3d.getX();
        y = vec3d.getY();
        z = vec3d.getZ();
    }
}
