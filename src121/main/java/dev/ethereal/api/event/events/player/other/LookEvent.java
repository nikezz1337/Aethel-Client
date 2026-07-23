package dev.ethereal.api.event.events.player.other;

import dev.ethereal.api.event.CancellableEvent;

public class LookEvent extends CancellableEvent {
    private double yaw;
    private double pitch;

    public LookEvent(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }
}
