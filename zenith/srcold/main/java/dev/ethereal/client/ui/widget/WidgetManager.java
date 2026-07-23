package dev.ethereal.client.ui.widget;

import lombok.Getter;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.client.features.modules.render.InterfaceModule;
import dev.ethereal.client.ui.widget.overlay.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WidgetManager {
    @Getter private final static WidgetManager instance = new WidgetManager();

    private final List<Widget> widgets = new ArrayList<>();

    public void load() {
        register(
                new WatermarkWidget(),
                new ArrayListWidget(),
                new KeybindsWidget(),
                new PotionsWidget(),
                new StaffsWidget(),
                new CooldownsWidget(),
                new NotifWidget(),

                new ArmorWidget(),

                new TargetInfoWidget(),

                new XYZWidget()
        );

        InterfaceModule.getInstance().init();

        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (InterfaceModule.getInstance().isEnabled()) {
                for (Widget widget : widgets) {
                    if (widget.isEnabled()) widget.render(event);
                }
            }
        }));
    }

    public void register(Widget... widgets) {
        this.widgets.addAll(List.of(widgets));
    }
}
