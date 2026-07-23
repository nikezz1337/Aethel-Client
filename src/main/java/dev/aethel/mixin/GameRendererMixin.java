package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.module.list.render.Removals;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void aethel$hideTotemAnimation(ItemStack stack, CallbackInfo ci) {
        if (stack == null || !stack.isOf(Items.TOTEM_OF_UNDYING)) return;

        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isTotemAnimationDisabled()) {
            ci.cancel();
        }
    }
}
