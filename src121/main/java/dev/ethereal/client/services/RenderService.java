package dev.ethereal.client.services;

import lombok.Getter;
import lombok.Setter;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.other.WindowResizeEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.client.features.modules.render.InterfaceModule;

@Getter
public class RenderService implements QuickImports {
    @Getter private static final RenderService instance = new RenderService();

    @Setter private float scale = 1.0f;

    private boolean updatingScale;

    public void load() {
        Events.subscribe(this);
    }

    @EventHandler
    public void onWindowResize(WindowResizeEvent event) {
        register();
    }

    private void register() {
        updatingScale = true;
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (updatingScale) {
            updateScale();
        }
    }

    public float scaled(float value) {
        return value * scale;
    }

    public void updateScale() {
        float w = mc.getWindow().getScaledWidth();
        float h = mc.getWindow().getScaledHeight();

        float bW = 1366f / 2f;
        float bH = 768f / 2f;

        float newScale = Math.max(w / bW, h / bH) * InterfaceModule.getScale();

        if (scale == newScale) {
            this.scale = newScale;
            updatingScale = false;
            return;
        }

        scale = MathUtil.interpolate(scale, newScale, 0.15f);
    }
}