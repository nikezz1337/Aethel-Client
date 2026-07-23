package dev.aethel.mixin;

import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.event.list.EventSprintSync;
import dev.aethel.module.list.movement.Sprint;
import dev.aethel.module.list.player.NoPush;
import dev.aethel.util.base.Instance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Final @Shadow protected MinecraftClient client;
    @Final @Shadow public ClientPlayNetworkHandler networkHandler;
    @Shadow public abstract void closeScreen();

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        new EventPlayerUpdate().post();
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        var noPush = Instance.get(NoPush.class);
        if (noPush != null && noPush.isEnabled() && noPush.collisionList.getValue("Блоки")) {
            ci.cancel();
        }
    }

    @Inject(
            method = "sendMovementPackets",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendSprintingPacket()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void onSprint(CallbackInfo ci) {
        new EventSprintSync().post();
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
        if (player.isUsingItem()) {
            var event = new dev.aethel.event.list.EventNoSlow();
            event.post();
            return player.isUsingItem() && player.getVehicle() == null && !event.isCancelled();
        } else {
            return player.isUsingItem() && player.getVehicle() == null;
        }
    }


}