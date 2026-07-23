package dev.aethel.event.list;

import dev.aethel.event.Event;

public class MoveInputEvent extends Event {

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

    public void cancel() {
        setCancelled(true);
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
