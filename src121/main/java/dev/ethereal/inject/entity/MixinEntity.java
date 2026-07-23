package dev.ethereal.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.event.events.player.move.VelocityEvent;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.client.features.modules.combat.NoPushModule;
import dev.ethereal.client.features.modules.player.FreecamModule;
import dev.ethereal.client.features.modules.render.RemovalsModule;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @ModifyExpressionValue(method = "updateMovementInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;getVelocity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d noPushInLiquidsHook(Vec3d original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        return !NoPushModule.getInstance().cancelPush(NoPushModule.PushingSource.LIQUIDS) ? original : Vec3d.ZERO;
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void freecamLookHook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this != SharedClass.player()) return;

        FreecamModule freecam = FreecamModule.getInstance();
        if (!freecam.isEnabled()) return;

        freecam.applyMouseInput(cursorDeltaX, cursorDeltaY);
        ci.cancel();
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public void cancelGlowingHook(CallbackInfoReturnable<Boolean> cir) {
        if (RemovalsModule.getInstance().isGlowEffect()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean fixFallDistance(boolean original) {
        if ((Object) this == SharedClass.player()) {
            return false;
        }

        return original;
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d updateVelocityHook(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == SharedClass.player()) {
            Entity self = (Entity) (Object) this;
            TravelEvent travelEvent = new TravelEvent(yaw, self.getPitch());
            Events.post(travelEvent);

            VelocityEvent event = new VelocityEvent(movementInput, speed, travelEvent.getYaw(), Entity.movementInputToVelocity(movementInput, speed, travelEvent.getYaw()));
            Events.post(event);
            return event.getVelocity();
        }


        return Entity.movementInputToVelocity(movementInput, speed, yaw);
    }
}
