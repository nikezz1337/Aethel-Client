package dev.ethereal.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import dev.ethereal.api.event.Listener;
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
        keyEvent();
        render2dEvent();
        tickEvent();
        screenEvent();
    }

    private void screenEvent() {
        ScreenEvent.getInstance().subscribe(new Listener<>(event -> {
            ScreenUtil.drawButton(event);
        }));
    }

    private void tickEvent() {
        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            SlownessManager.tick();

            ConfigSkin.getInstance().fetchSkin();
        }));
    }

    private void render2dEvent() {
        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.currentScreen instanceof ChatScreen) {
                DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                    if (draggable.getModule().isEnabled()) {
                        draggable.onDraw();
                    }
                });
            }

            GpsManager.getInstance().update(event.context());
        }));
    }

    private void keyEvent() {
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
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
        }));
    }
}
