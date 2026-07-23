package dev.aethel.mixin;

import dev.aethel.module.list.render.SwingAnimations;
import dev.aethel.util.base.Instance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        var swing = Instance.get(SwingAnimations.class);
        if (swing != null && swing.isEnabled() && swing.swingEnabled.getValue() && swing.smoothEnabled.getValue()) {
            cir.setReturnValue((int) swing.slowAnimationSpeed.getIntValue());
        }
    }
}
