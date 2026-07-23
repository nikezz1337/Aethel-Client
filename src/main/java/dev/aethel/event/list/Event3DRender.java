package dev.aethel.event.list;

import dev.aethel.event.Event;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class Event3DRender extends Event {
    private final MatrixStack matrixStack;
    private final Camera camera;
    private final float tickDelta;
    private final Matrix4f projectionMatrix;

    public Event3DRender(MatrixStack matrixStack, Camera camera, float tickDelta) {
        this(matrixStack, camera, tickDelta, new Matrix4f());
    }

    public Event3DRender(MatrixStack matrixStack, Camera camera, float tickDelta, Matrix4f projectionMatrix) {
        this.matrixStack = matrixStack;
        this.camera = camera;
        this.tickDelta = tickDelta;
        this.projectionMatrix = projectionMatrix;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public Camera getCamera() {
        return camera;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
