package antileak.base.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import antileak.base.elysium;
import antileak.base.api.events.implement.EventPacket;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.baritone.BaritoneAntiStuck;
import antileak.base.api.utils.bot.BotSessionManager;
import antileak.base.api.utils.chat.ChatUtils;
import antileak.base.client.modules.impl.render.base.implement.Cooldowns;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Shadow
    private ClientWorld world;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void sendChatMessage(@NotNull String message, CallbackInfo ci) {
        if (message.startsWith(elysium.INSTANCE.commandStorage.getPrefix())) {
            try {
                elysium.INSTANCE.commandStorage.getDispatcher().execute(message.substring(elysium.INSTANCE.commandStorage.getPrefix().length()), elysium.INSTANCE.commandStorage.getSource());
            } catch (CommandSyntaxException e) {
                ChatUtils.sendMessage(Formatting.RED + "Ошибка в использовании!");
            }
            ci.cancel();
            return;
        }
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void onVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        EventPacket event = new EventPacket(packet, EventPacket.Type.RECEIVE);
        event.call();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onExplosion", at = @At("HEAD"), cancellable = true)
    private void onExplosion(ExplosionS2CPacket packet, CallbackInfo ci) {
        EventPacket event = new EventPacket(packet, EventPacket.Type.RECEIVE);
        event.call();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onEntityPositionSync", at = @At("HEAD"), cancellable = true)
    private void onEntityPositionSync(EntityPositionSyncS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (world == null || mc.player == null || mc.world == null) {
            ci.cancel();
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        BaritoneAntiStuck.onGameMessage(packet.content().getString());
        Cooldowns.onGameMessage(packet.content().getString());
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        BotSessionManager.finishBotConnectStage();
    }
}
