package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.DeathScreen;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Respawn", category = Category.OTHER)
public class AutoRespawnModule extends Module {
    @Getter private static final AutoRespawnModule instance = new AutoRespawnModule();

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.currentScreen instanceof DeathScreen) {
                if (mc.player.deathTime > 2) {
                    mc.player.requestRespawn();
                    mc.setScreen(null);
                }
            }
        }));

        addEvents(tickEvent);
    }
}
