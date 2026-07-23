package dev.aethel.mixin;

import dev.aethel.module.list.player.FreeCamera;
import dev.aethel.util.base.Instance;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraFreeCameraMixin {

    @Shadow protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdateTail(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        FreeCamera freeCamera = Instance.get(FreeCamera.class);
        if (freeCamera != null && freeCamera.isEnabled() && freeCamera.getCameraPos() != null) {
            var pos = freeCamera.getCameraPos();
            setPos(pos.x, pos.y - 0.3, pos.z);
        }
    }
}
