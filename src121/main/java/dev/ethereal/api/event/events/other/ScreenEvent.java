package dev.ethereal.api.event.events.other;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public class ScreenEvent {
    private final Screen screen;
    private final List<ButtonWidget> buttons = new ArrayList<>();

    public ScreenEvent(Screen screen) {
        this.screen = screen;
    }
}
