package dev.ethereal.api.event.events.player.move;

import dev.ethereal.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MotionEvent extends CancellableEvent {
    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;
    public static float lastYaw, lastPitch;

    public void setYaw(float yaw) {
        this.yaw = yaw;
        lastYaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        lastPitch = pitch;
    }

    public void ground(boolean onGround) {
        this.onGround = onGround;
    }
}
