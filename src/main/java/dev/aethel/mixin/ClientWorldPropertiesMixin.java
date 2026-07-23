package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.module.list.render.WorldTweaks;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {

    @Shadow private long timeOfDay;

    @Inject(method = "setTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void aethel$setTimeOfDay(long timeOfDay, CallbackInfo ci) {
        WorldTweaks tweaks = Aethel.getInstance().getModuleStorage().get(WorldTweaks.class);
        if (tweaks == null || !tweaks.isTimeEnabled()) return;

        this.timeOfDay = tweaks.getForcedTime();
        ci.cancel();
    }
}
