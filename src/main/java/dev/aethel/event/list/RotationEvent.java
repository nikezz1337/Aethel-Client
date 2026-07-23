package dev.aethel.event.list;

import dev.aethel.event.Event;

public class RotationEvent extends Event {
    private float yaw;
    private float pitch;
    private final float partialTicks;

    public RotationEvent(float yaw, float pitch, float partialTicks) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.partialTicks = partialTicks;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
