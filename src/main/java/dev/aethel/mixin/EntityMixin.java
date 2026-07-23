package dev.aethel.mixin;

import dev.aethel.module.list.player.NoPush;
import dev.aethel.util.base.Instance;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void onPushAwayFrom(Entity entity, CallbackInfo ci) {
        if ((Object) this != net.minecraft.client.MinecraftClient.getInstance().player) return;
        var noPush = Instance.get(NoPush.class);
        if (noPush != null && noPush.isEnabled() && noPush.collisionList.getValue("Игроки")) {
            ci.cancel();
        }
    }

    @Inject(method = "isPushedByFluids", at = @At("RETURN"), cancellable = true)
    private void onIsPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != net.minecraft.client.MinecraftClient.getInstance().player) return;
        var noPush = Instance.get(NoPush.class);
        if (noPush != null && noPush.isEnabled() && noPush.collisionList.getValue("Вода")) {
            cir.setReturnValue(false);
        }
    }
}
