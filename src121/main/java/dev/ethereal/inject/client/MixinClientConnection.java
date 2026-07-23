package dev.ethereal.inject.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.utils.other.NetworkUtil;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
    private void sendPackets(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo callbackInfo) {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null ||
                NetworkUtil.silentPacket()) return;

        PacketEvent event = Events.post(new PacketEvent(packet, PacketEvent.PacketType.SEND));
        // These packets carry a client-side sequence id. Dropping one after vanilla has
        // reserved its sequence creates gaps and Grim reports BadPacketsH.
        if (event.isCancelled() && !isSequencedActionPacket(packet)) {
            callbackInfo.cancel();
        }
    }

    @Unique
    private static boolean isSequencedActionPacket(Packet<?> packet) {
        return packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PlayerInteractBlockC2SPacket
                || packet instanceof PlayerActionC2SPacket;
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void receivePackets(Packet<?> packet, PacketListener listener, CallbackInfo callbackInfo) {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null) return;

        if (packet instanceof BundleS2CPacket) {
            // Bundles are atomic in vanilla. Canceling the entire bundle because one
            // inner packet looked optional can desync inventory, world, or velocity state.
            handleSinglePacket(packet);
            return;
        }

        if (handleSinglePacket(packet)) {
            callbackInfo.cancel();
        }
    }

    @Unique
    private static boolean handleSinglePacket(Packet<?> packet) {
        PacketEvent event = Events.post(new PacketEvent(packet, PacketEvent.PacketType.RECEIVE));
        return event.isCancelled();
    }
}
