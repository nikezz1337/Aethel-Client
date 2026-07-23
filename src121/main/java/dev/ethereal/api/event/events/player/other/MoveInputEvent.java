package dev.ethereal.api.event.events.player.other;

import dev.ethereal.api.event.CancellableEvent;

public class MoveInputEvent extends CancellableEvent {

    public float forward;
    public float strafe;
    public boolean jump;
    public boolean sneak;
    public double sneakSlow;

    public MoveInputEvent(float forward, float strafe, boolean jump, boolean sneak, double sneakSlow) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.sneakSlow = sneakSlow;
    }

    public float getForward() {
        return forward;
    }

    public float getStrafe() {
        return strafe;
    }

    public boolean isJump() {
        return jump;
    }

    public boolean isSneaking() {
        return sneak;
    }

    public double getSneakSlow() {
        return sneakSlow;
    }
}
