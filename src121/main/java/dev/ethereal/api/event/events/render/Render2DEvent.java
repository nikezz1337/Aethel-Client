package dev.ethereal.api.event.events.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class Render2DEvent {
    private final DrawContext context;
    private final MatrixStack matrixStack;
    private final float partialTicks;
}
