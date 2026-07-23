package dev.aethel.mixin;

import dev.aethel.config.CommandHandler;
import dev.aethel.event.list.EventPacket;
import dev.aethel.module.list.render.particles.ParticlesModule;
import dev.aethel.util.base.Instance;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (CommandHandler.handle(message)) ci.cancel();
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        EventPacket event = new EventPacket(packet, EventPacket.Type.RECEIVE);
        event.post();
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) return;
        ParticlesModule module = Instance.get(ParticlesModule.class);
        if (module == null || !module.isEnabled()) return;
        Entity entity = packet.getEntity(((ClientPlayNetworkHandler) (Object) this).getWorld());
        if (entity == null) return;
        module.spawnTotemParticles(entity);
    }
}
