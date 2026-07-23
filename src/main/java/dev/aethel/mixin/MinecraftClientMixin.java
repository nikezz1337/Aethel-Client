package dev.aethel.mixin;

import dev.aethel.event.list.EventMinecraftInit;
import dev.aethel.event.list.EventTick;
import dev.aethel.event.list.EventTickEnd;
import dev.aethel.event.list.WorldChangeEvent;
import dev.aethel.event.list.WorldLoadEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/client/MinecraftClient;Ljava/io/File;)Lnet/minecraft/client/option/GameOptions;"))
    private void initOptions(RunArgs args, CallbackInfo ci) {
        new EventMinecraftInit().post();
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void tick(CallbackInfo ci) {
        new EventTick().post();
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void tickEnd(CallbackInfo ci) {
        new EventTickEnd().post();
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void onWorldChange(ClientWorld world, CallbackInfo ci) {
        new WorldChangeEvent().post();
    }

    @Inject(method = "joinWorld", at = @At("HEAD"))
    private void onWorldLoad(ClientWorld world, DownloadingTerrainScreen.WorldEntryReason reason, CallbackInfo ci) {
        new WorldLoadEvent().post();
    }
}
