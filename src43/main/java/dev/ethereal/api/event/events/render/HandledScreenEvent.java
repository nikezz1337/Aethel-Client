package dev.ethereal.api.event.events.render;

import dev.ethereal.api.event.events.Event;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HandledScreenEvent extends Event<HandledScreenEvent> {
    DrawContext drawContext;
    Slot slotHover;
    int backgroundWidth, backgroundHeight;
    int mouseX, mouseY;
    float tickDelta;
    
    private static final HandledScreenEvent INSTANCE = new HandledScreenEvent(null, null, 0, 0, 0, 0, 0);
    
    public static HandledScreenEvent getInstance() {
        return INSTANCE;
    }
}
