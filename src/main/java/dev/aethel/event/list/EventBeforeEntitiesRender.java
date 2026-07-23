package dev.aethel.event.list;

import dev.aethel.event.Event;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;

public class EventBeforeEntitiesRender extends Event {
    private final Matrix4f positionMatrix;
    private final Matrix4f projectionMatrix;
    private final Camera camera;
    private final float tickDelta;

    public EventBeforeEntitiesRender(Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
        this.positionMatrix = positionMatrix;
        this.projectionMatrix = projectionMatrix;
        this.camera = camera;
        this.tickDelta = tickDelta;
    }

    public Matrix4f getPositionMatrix() { return positionMatrix; }
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
    public Camera getCamera() { return camera; }
    public float getTickDelta() { return tickDelta; }
}
