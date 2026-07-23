package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.DeathScreen;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Respawn", category = Category.OTHER)
public class AutoRespawnModule extends Module {
    @Getter private static final AutoRespawnModule instance = new AutoRespawnModule();

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.currentScreen instanceof DeathScreen) {
                mc.player.requestRespawn();
                mc.setScreen(null);
        }
    }
}
