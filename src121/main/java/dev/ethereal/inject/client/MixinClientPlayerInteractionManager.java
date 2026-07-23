package dev.ethereal.inject.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.player.world.ClickSlotEvent;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    public void onClickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null) return;
        ClickSlotEvent event = new ClickSlotEvent(actionType, slotId, button, syncId);
        Events.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
