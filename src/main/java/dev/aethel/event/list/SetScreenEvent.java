package dev.aethel.event.list;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screen.Screen;
import dev.aethel.event.Event;

@Getter
@Setter
public class SetScreenEvent extends Event {
    private Screen screen;

    public SetScreenEvent(Screen screen) {
        this.screen = screen;
    }
}
