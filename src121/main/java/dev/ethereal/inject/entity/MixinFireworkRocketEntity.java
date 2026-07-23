package dev.ethereal.inject.entity;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.system.backend.Pair;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.client.features.modules.movement.nitrofirework.NitroFireworkModule;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {
    @Shadow
    private LivingEntity shooter;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d fixFireworkVelocity(LivingEntity instance) {
        if (instance != SharedClass.player()) {
            return instance.getRotationVector();
        }

        TravelEvent event = new TravelEvent(instance.getYaw(), instance.getPitch());
        Events.post(event);
        return instance.getRotationVector(event.getPitch(), event.getYaw());
    }


    @ModifyArgs(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;", ordinal = 0))
    private void hookExtendedFirework(Args args, @Local(ordinal = 0) Vec3d rotation, @Local(ordinal = 1) Vec3d velocity) {
        if (shooter != SharedClass.player() || !NitroFireworkModule.getInstance().isEnabled()) return;

        Pair<Float, Float> pair = NitroFireworkModule.getInstance().currentMode.velocityValues();

        float donichka = 0.1f;
        float piska = 0.5f;
        Vec2f multiplier = new Vec2f(pair.left(), pair.right());
        args.set(0, rotation.x * donichka + (rotation.x * multiplier.x - velocity.x) * piska);
        args.set(1, rotation.y * donichka + (rotation.y * multiplier.y - velocity.y) * piska);
        args.set(2, rotation.z * donichka + (rotation.z * multiplier.x - velocity.z) * piska);
    }
}
