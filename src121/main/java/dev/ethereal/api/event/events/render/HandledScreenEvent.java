package dev.ethereal.api.event.events.render;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HandledScreenEvent {
    DrawContext drawContext;
    Slot slotHover;
    int backgroundWidth, backgroundHeight;
    int mouseX, mouseY;
    float tickDelta;
}
