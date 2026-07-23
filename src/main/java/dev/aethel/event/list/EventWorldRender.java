package dev.aethel.event.list;

import net.minecraft.client.util.math.MatrixStack;
import dev.aethel.event.Event;

public class EventWorldRender extends Event {
    private final MatrixStack matrixStack;
    private final float tickDelta;

    public EventWorldRender(MatrixStack matrixStack, float tickDelta) {
        this.matrixStack = matrixStack;
        this.tickDelta = tickDelta;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}
