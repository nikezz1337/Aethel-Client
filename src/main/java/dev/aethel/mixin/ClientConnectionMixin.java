package dev.aethel.mixin;

import dev.aethel.event.list.EventPacket;
import dev.aethel.util.packet.NetworkUtils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (NetworkUtils.getSilentPackets().remove(packet)) return;

        EventPacket event = new EventPacket(packet, EventPacket.Type.SEND);
        event.post();
        if (event.isCancelled()) ci.cancel();
    }
}
