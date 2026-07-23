package dev.ethereal.client.ui.widget;

import lombok.Getter;
import lombok.Setter;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.system.draggable.Draggable;
import dev.ethereal.api.system.draggable.DraggableManager;
import dev.ethereal.api.system.interfaces.IRenderer;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.render.fonts.Font;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.features.modules.render.InterfaceModule;
import dev.ethereal.client.services.RenderService;

@Getter
@Setter
public abstract class Widget implements QuickImports, IRenderer {
    protected Widget(float x, float y) {
        this.draggable = create(x, y, getName());
    }

    private final Easing easing = Easing.SINE_OUT;
    private final long duration = 100;

    public abstract String getName();
    private final Draggable draggable;
    private boolean enabled;

    private Draggable create(float x, float y, String name) {
        return DraggableManager.getInstance().create(InterfaceModule.getInstance(), name, x, y);
    }

    public void render(Render2DEvent event) {
        render(event.matrixStack());
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    public float getScale() { return RenderService.getInstance().getScale(); }
    public float getGap() { return scaled(3f); }
    public Font getMediumFont() { return Fonts.SF_MEDIUM; }
    public Font getSemiBoldFont() { return Fonts.PS_BOLD; }
}
