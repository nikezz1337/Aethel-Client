package dev.ethereal.api.module;

import lombok.Getter;
import lombok.Setter;
import dev.ethereal.api.system.backend.Configurable;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.client.features.modules.other.ToggleSoundsModule;
import dev.ethereal.client.features.modules.render.ClickGUIModule;

@Getter
public abstract class Module extends Configurable implements QuickImports {
    private final String name;
    private final Category category;
    @Setter private int bind;

    private boolean enabled;

    public Module() {
        ModuleRegister data = getClass().getAnnotation(ModuleRegister.class);

        if (data == null) try {
            throw new Exception("No data for " + getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.name = data.name();
        this.category = data.category();
        this.bind = data.bind();
    }

    public boolean hasBind() { return bind != -999; }

    public void toggle() {
        setEnabled(!enabled, false, false);
    }

    public void toggle(boolean fromBind) {
        setEnabled(!enabled, false, fromBind);
    }

    public void setEnabled(boolean newState) {
        setEnabled(newState, false, false);
    }

    public void setEnabled(boolean newState, boolean config) {
        setEnabled(newState, config, false);
    }

    public void setEnabled(boolean newState, boolean config, boolean fromBind) {
        if (enabled == newState) return;

        enabled = newState;
        if (enabled) {
            onEnable();
            onEvent();
        } else {
            onDisable();
            removeAllEvents();
        }

        if (config || this instanceof ClickGUIModule) return;
        ToggleSoundsModule.playToggle(newState);

        if (fromBind) {
            try {
                dev.ethereal.client.ui.widget.WidgetManager widgetManager = dev.ethereal.client.ui.widget.WidgetManager.getInstance();
                dev.ethereal.client.ui.widget.overlay.NotifWidget widget = (dev.ethereal.client.ui.widget.overlay.NotifWidget) widgetManager.getWidgets().stream()
                        .filter(w -> w instanceof dev.ethereal.client.ui.widget.overlay.NotifWidget)
                        .findFirst()
                        .orElse(null);

                if (widget != null && widget.moduleState) {
                    widget.addNotif(name + (newState ? " §aвключен" : " §cвыключен"));
                }
            } catch (Exception ignored) {}
        }
    }

    public abstract void onEvent();

    public void onEnable() {}
    public void onDisable() {}
}
