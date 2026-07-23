package dev.ethereal.client.features.modules.movement.spider;

import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.system.backend.Choice;
import dev.ethereal.api.system.backend.Configurable;

public abstract class SpiderMode extends Choice {
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    public boolean hozColl() {
        return mc.player.horizontalCollision;
    }
}
