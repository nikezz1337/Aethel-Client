package dev.aethel.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvoker {
    @Invoker("clipToSpace")
    float invokeClipToSpace(float distance);

    @Invoker("setPos")
    void invokeSetPos(double x, double y, double z);
}
