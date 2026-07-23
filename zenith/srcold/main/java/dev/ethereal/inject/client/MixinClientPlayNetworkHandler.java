package dev.ethereal.inject.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.command.CommandManager;
import dev.ethereal.api.utils.combat.TpsCalculator;
import dev.ethereal.client.features.modules.render.particles.ParticlesModule;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sendChatMessage(String content, CallbackInfo ci) {
        CommandManager.getInstance().executeCommands(content, ci);
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    private void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        TpsCalculator.getInstance().onTimeUpdate();
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != 35) return;
        ParticlesModule module = ParticlesModule.getInstance();
        if (!module.isEnabled()) return;
        Entity entity = packet.getEntity(((net.minecraft.client.network.ClientPlayNetworkHandler) (Object) this).getWorld());
        if (entity == null) return;
        module.spawnTotemParticles(entity);
    }
}
