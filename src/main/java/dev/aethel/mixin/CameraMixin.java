package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.event.list.RotationEvent;
import dev.aethel.module.list.player.NoPush;
import dev.aethel.util.base.Instance;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
            )
    )
    private void redirectSetRotation(Camera instance, float yaw, float pitch, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        RotationEvent event = new RotationEvent(yaw, pitch, tickDelta);
        Aethel.getInstance().getEventBus().post(event);

        float newYaw = event.getYaw();
        float newPitch = event.getPitch();

        if (thirdPerson && inverseView) {
            newYaw += 180.0F;
            newPitch = -newPitch;
        }

        instance.setRotation(newYaw, newPitch);
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"
            )
    )
    private float redirectClipToSpace(Camera instance, float distance, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        var noPush = Instance.get(NoPush.class);
        if (noPush != null && noPush.isEnabled() && noPush.collisionList.getValue("Блоки")) {
            return distance;
        }
        return ((CameraInvoker) instance).invokeClipToSpace(distance);
    }
}
