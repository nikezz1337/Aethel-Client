package dev.ethereal.inject.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.player.other.RotationEvent;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import dev.ethereal.client.features.modules.render.CameraClipModule;
import dev.ethereal.client.features.modules.player.FreecamModule;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    protected abstract float clipToSpace(float desiredCameraDistance);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void freecamUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        FreecamModule freecam = FreecamModule.getInstance();
        if (!freecam.isEnabled()) return;
        this.setRotation(freecam.getYaw(), freecam.getPitch());
        Vec3d p = freecam.getRenderPos(tickDelta);
        this.setPos(p.x, p.y, p.z);
    }

    @ModifyReturnValue(method = "isThirdPerson", at = @At("RETURN"))
    private boolean freecamThirdPerson(boolean original) {
        return original || FreecamModule.getInstance().isEnabled();
    }

    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;moveBy(FFF)V", ordinal = 0))
    private void update(Args args) {
        if (CameraClipModule.getInstance().isEnabled()) {
            args.set(0, -clipToSpace(CameraClipModule.getInstance().distance.getValue()));
        }
    }

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void redirectSetRotation(Camera instance, float yaw, float pitch, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        RotationEvent event = new RotationEvent(yaw, pitch, tickDelta);
        Events.post(event);

        float newYaw = event.getYaw();
        float newPitch = event.getPitch();

        if (thirdPerson && inverseView) {
            newYaw += 180.0F;
            newPitch = -newPitch;
        }

        instance.setRotation(newYaw, newPitch);
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void clipToSpace(float f, CallbackInfoReturnable<Float> returnable) {
        if (CameraClipModule.getInstance().isEnabled()) {
            returnable.setReturnValue(CameraClipModule.getInstance().distance.getValue());
        }
    }
}
