package dev.ethereal.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ethereal.client.features.modules.movement.WaterSpeedModule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.player.move.JumpEvent;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.client.features.modules.combat.NoPushModule;
import dev.ethereal.client.features.modules.render.SwingAnimationModule;

import static dev.ethereal.api.system.interfaces.QuickImports.mc;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @Shadow
    public abstract boolean isGliding();

    @Shadow
    public int jumpingCooldown;

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getArmSwingAnimationEnd(final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        SwingAnimationModule swingAnim = SwingAnimationModule.getInstance();
        if (swingAnim.slow.getValue() && swingAnim.isEnabled())
            callbackInfoReturnable.setReturnValue(swingAnim.speed.getValue().intValue());
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void noPushHookByEntity(CallbackInfo ci) {
        if (NoPushModule.getInstance().cancelPush(NoPushModule.PushingSource.ENTITY)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    public float fixJumpVelocity(float original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        JumpEvent event = new JumpEvent(original);
        Events.post(event);
        return event.getYaw();
    }

    @ModifyExpressionValue(method = "calcGlidingVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d fixGlidingVelocityVector(Vec3d original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        TravelEvent event = new TravelEvent(self.getYaw(), self.getPitch());
        Events.post(event);
        return self.getRotationVector(event.getPitch(), event.getYaw());
    }

    @ModifyVariable(
            method = {"setSprinting"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private boolean setSprintingHook(boolean sprinting) {
        if (WaterSpeedModule.getInstance().isEnabled() && mc.player.isTouchingWater()) {
            boolean forward = mc.player.input.movementForward > 0.0F;
            boolean backward = mc.player.input.movementForward < 0.0F;
            return forward && !backward;
        } else {
            return sprinting;
        }
    }
}
