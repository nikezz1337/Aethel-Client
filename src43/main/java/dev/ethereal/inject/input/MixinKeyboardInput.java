package dev.ethereal.inject.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ethereal.api.utils.player.PlayerUtil;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.event.events.player.move.SprintEvent;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.api.utils.player.DirectionalInput;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import dev.ethereal.client.features.modules.combat.AuraModule;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput onTick(PlayerInput original) {
        MovementInputEvent.MovementInputEventData movementInputEvent = new MovementInputEvent.MovementInputEventData(original, original.jump(), original.sneak(), new DirectionalInput(original));
        MovementInputEvent.getInstance().call(movementInputEvent);

        DirectionalInput untransformedDirectionalInput = movementInputEvent.getDirectionalInput();
        DirectionalInput directionalInput = transformDirection(untransformedDirectionalInput);

        SprintEvent.SprintEventData sprintEvent = new SprintEvent.SprintEventData(directionalInput);
        SprintEvent.getInstance().call(sprintEvent);

        this.untransformed = new PlayerInput(
                untransformedDirectionalInput.isForwards(),
                untransformedDirectionalInput.isBackwards(),
                untransformedDirectionalInput.isLeft(),
                untransformedDirectionalInput.isRight(),
                original.jump(),
                original.sneak(),
                sprintEvent.isSprint()
        );

        return new PlayerInput(
                directionalInput.isForwards(),
                directionalInput.isBackwards(),
                directionalInput.isLeft(),
                directionalInput.isRight(),
                movementInputEvent.isJump(),
                movementInputEvent.isSneak(),
                sprintEvent.isSprint()
        );
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput input) {
        ClientPlayerEntity player = SharedClass.player();
        RotationComponent RotationComponent = dev.ethereal.api.utils.rotation.manager.RotationComponent.getInstance();
        Rotation rotation = RotationComponent.currentRotation();

        if (!RotationComponent.isRotating() || rotation == null || player == null) {
            return input;
        }

        float z = KeyboardInput.getMovementMultiplier(input.isForwards(), input.isBackwards());
        float x = KeyboardInput.getMovementMultiplier(input.isLeft(), input.isRight());

        float realYaw = RotationComponent.getPrevRealYaw();

        AuraModule auraModule = AuraModule.getInstance();

        if (auraModule.isEnabled() && auraModule.moveCorrection.getValue() && auraModule.target != null) {
            boolean isSpooky = PlayerUtil.isST();
            if (isSpooky) {
                Box playerBox = player.getBoundingBox();
                Box targetBox = auraModule.target.getBoundingBox();
                if (playerBox.intersects(targetBox)) {
                Vec3d pushDir = player.getPos().subtract(auraModule.target.getPos());
                if (pushDir.lengthSquared() < 1e-8) pushDir = new Vec3d(1, 0, 0);
                pushDir = pushDir.normalize();
                double pushAngle = Math.toDegrees(Math.atan2(-pushDir.x, pushDir.z));
                double delta = MathHelper.wrapDegrees(pushAngle - rotation.getYaw());
                float forward = (float) Math.cos(Math.toRadians(delta));
                float strafe = (float) Math.sin(Math.toRadians(delta));
                    return new DirectionalInput(forward, strafe);
                }
            }

            float sentYaw = rotation.getYaw();

            if (auraModule.correctionMode.is("Свободная")) {
                if (z == 0 && x == 0) return input;

                double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(MoveUtil.direction(realYaw, z, x)));

                float bestForward   = 0F;
                float bestStrafe    = 0F;
                float minDifference = Float.MAX_VALUE;

                for (float forward = -1F; forward <= 1F; forward += 1F) {
                    for (float strafe = -1F; strafe <= 1F; strafe += 1F) {
                        if (forward == 0F && strafe == 0F) continue;

                        double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(MoveUtil.direction(sentYaw, forward, strafe)));
                        double difference = Math.abs(MathHelper.wrapDegrees((float)(targetAngle - predictedAngle)));

                        if (difference < minDifference) {
                            minDifference = (float) difference;
                            bestForward   = forward;
                            bestStrafe    = strafe;
                        }
                    }
                }
                return new DirectionalInput(bestForward, bestStrafe);
            } else {
                if (z == 0 && x == 0) return input;

                Vec3d position = auraModule.target.getPos();
                double deltaX = position.x - player.getX();
                double deltaZ = position.z - player.getZ();

                double angleToTarget = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0);
                double inputAngle = Math.toDegrees(Math.atan2(-x, z));
                float intendedAngle = MathHelper.wrapDegrees((float)(angleToTarget + inputAngle));

                float bestForward   = 0F;
                float bestStrafe    = 0F;
                float minDifference = Float.MAX_VALUE;

                for (float forward = -1F; forward <= 1F; forward += 1F) {
                    for (float strafe = -1F; strafe <= 1F; strafe += 1F) {
                        if (forward == 0F && strafe == 0F) continue;

                        double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(MoveUtil.direction(sentYaw, forward, strafe)));
                        double difference = Math.abs(MathHelper.wrapDegrees((float)(intendedAngle - predictedAngle)));

                        if (difference < minDifference) {
                            minDifference = (float) difference;
                            bestForward   = forward;
                            bestStrafe    = strafe;
                        }
                    }
                }
                return new DirectionalInput(bestForward, bestStrafe);
            }
        }

        return input;
    }
}
