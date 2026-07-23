package dev.ethereal.api.event;

import dev.ethereal.api.event.events.Event;
import dev.ethereal.api.event.events.client.PacketEvent;
import lombok.Generated;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public class ScreenCloseEvent extends Event {

    @Getter
    private static final PacketEvent instance = new PacketEvent();
    private Screen screen;
    private int windowId;

    @Generated
    public Screen getScreen() {
        return this.screen;
    }

    @Generated
    public int getWindowId() {
        return this.windowId;
    }

    @Generated
    public ScreenCloseEvent(Screen screen, int windowId) {
        this.screen = screen;
        this.windowId = windowId;
    }
}
