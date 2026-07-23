package dev.ethereal.client.features.modules.movement.fly;

import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.system.backend.Choice;

public abstract class FlightMode extends Choice {


    // events
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    // module methods
    public void onEnable() {}
    public void onDisable() {}
    public void toggle() {}
}
