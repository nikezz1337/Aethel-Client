package dev.ethereal.client.features.modules.movement;

import lombok.Getter;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;

@ModuleRegister(name = "NoJumpDelay", category = Category.MOVEMENT)
public class NoJumpDelayModule extends Module {
    @Getter private static final NoJumpDelayModule instance = new NoJumpDelayModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            mc.player.jumpingCooldown = 0;
        }));

        addEvents(updateEvent);
    }
}
