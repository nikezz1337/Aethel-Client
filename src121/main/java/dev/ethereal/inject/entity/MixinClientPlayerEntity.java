package dev.ethereal.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.other.RotationUpdateEvent;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.event.events.player.other.CloseScreenEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.player.move.MoveEvent;
import dev.ethereal.api.event.events.player.move.SprintEvent;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.api.utils.player.DirectionalInput;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationManager;

import dev.ethereal.client.features.modules.combat.NoPushModule;
import dev.ethereal.client.features.modules.movement.noslow.NoSlowModule;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow public Input input;
    @Final @Shadow public ClientPlayNetworkHandler networkHandler;
    @Final @Shadow protected MinecraftClient client;
    @Shadow private double lastX, lastBaseY, lastZ;
    @Shadow private float lastYaw, lastPitch;
    @Shadow private boolean lastOnGround, lastHorizontalCollision, autoJumpEnabled;
    @Shadow private int ticksSinceLastPositionPacketSent;
    @Shadow protected boolean isCamera() { return false; }
    @Shadow private void sendSprintingPacket() {}

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHook(CallbackInfo ci) {
        Events.post(new UpdateEvent());
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void moveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        Events.post(event);
        if (event.isCancelled()) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @Overwrite
    public void sendMovementPackets() {
        this.sendSprintingPacket();
        if (!this.isCamera()) {
            return;
        }

        Events.post(new RotationUpdateEvent());
        MotionEvent event = new MotionEvent(getX(), getY(), getZ(), getYaw(), getPitch(), isOnGround());
        Events.post(event);
        if (event.isCancelled()) {
            return;
        }

        if (SharedClass.player() != null) {
            this.setHeadYaw(event.getYaw());
            this.setBodyYaw(event.getYaw());
        }

        double d = this.getX() - this.lastX;
        double e = this.getY() - this.lastBaseY;
        double f = this.getZ() - this.lastZ;
        double g = event.getYaw() - this.lastYaw;
        double h = event.getPitch() - this.lastPitch;
        ++this.ticksSinceLastPositionPacketSent;
        boolean positionChanged = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4)
                || this.ticksSinceLastPositionPacketSent >= 20;
        boolean rotationChanged = g != 0.0D || h != 0.0D;

        if (positionChanged && rotationChanged) {
            this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    this.getX(), this.getY(), this.getZ(),
                    event.getYaw(), event.getPitch(),
                    event.isOnGround(), this.horizontalCollision
            ));
        } else if (positionChanged) {
            this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    this.getX(), this.getY(), this.getZ(),
                    event.isOnGround(), this.horizontalCollision
            ));
        } else if (rotationChanged) {
            this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    event.getYaw(), event.getPitch(),
                    event.isOnGround(), this.horizontalCollision
            ));
        } else if (this.lastOnGround != event.isOnGround() || this.lastHorizontalCollision != this.horizontalCollision) {
            this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(
                    event.isOnGround(), this.horizontalCollision
            ));
        }

        if (positionChanged) {
            this.lastX = this.getX();
            this.lastBaseY = this.getY();
            this.lastZ = this.getZ();
            this.ticksSinceLastPositionPacketSent = 0;
        }

        if (rotationChanged) {
            this.lastYaw = event.getYaw();
            this.lastPitch = event.getPitch();
        }

        this.lastOnGround = event.isOnGround();
        this.lastHorizontalCollision = this.horizontalCollision;
        this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
        RotationManager.getInstance().updateServerRotation(new Rotation(event.getYaw(), event.getPitch()));
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprint()Z"))
    private boolean sprintEventTick(boolean original) {
        return sprintHook(original);
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean sprintEventInput(boolean original) {
        return sprintHook(original);
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void noPushByBlocksHook(double x, double d, CallbackInfo ci) {
        if (NoPushModule.getInstance().cancelPush(NoPushModule.PushingSource.BLOCK)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean tickMovementHook(boolean original) {
        if (NoSlowModule.getInstance().doUseNoSlow()) return false;
        return original;
    }

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean sprintAffectStartHook(boolean original) {
        if (NoSlowModule.getInstance().isEnabled()) return false;

        return original;
    }

    @Inject(method = "closeHandledScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"), cancellable = true)
    private void onCloseHandledScreen(CallbackInfo ci) {
        CloseScreenEvent event = new CloseScreenEvent();
        Events.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Unique
    private boolean sprintHook(boolean origin) {
        SprintEvent event = new SprintEvent(new DirectionalInput(input));
        event.setSprint(origin);
        Events.post(event);
        return event.isSprint();
    }
}
