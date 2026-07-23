package dev.ethereal.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.other.ScreenEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.ModuleManager;
import dev.ethereal.api.system.client.GpsManager;
import dev.ethereal.api.system.configs.ConfigSkin;
import dev.ethereal.api.system.configs.MacroManager;
import dev.ethereal.api.system.draggable.DraggableManager;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.other.ScreenUtil;
import dev.ethereal.api.utils.other.SlownessManager;

public class HeartbeatService implements QuickImports {
    @Getter private static final HeartbeatService instance = new HeartbeatService();

    public void load() {
        Events.subscribe(this);
    }

    @EventHandler
    public void onScreen(ScreenEvent event) {
        ScreenUtil.drawButton(event);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        SlownessManager.tick();

        ConfigSkin.getInstance().fetchSkin();
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.currentScreen instanceof ChatScreen) {
            DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                if (draggable.getModule().isEnabled()) {
                    draggable.onDraw();
                }
            });
        }

        GpsManager.getInstance().update(event.context());
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.action() != 1 || event.key() == -999 || event.key() == -1) return;

        int action = event.action();
        int key = event.key() + (event.mouse() ? -100 : 0);

        if (mc.currentScreen == null) {
            ModuleManager.getInstance().getModules().forEach(module -> {
                int bind = module.getBind();
                if (bind == key && module.hasBind()) {
                    module.toggle(true);
                }
            });

            MacroManager.getInstance().onKeyPressed(key);
        }
    }
}
