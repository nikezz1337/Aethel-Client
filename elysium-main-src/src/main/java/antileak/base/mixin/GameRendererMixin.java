package antileak.base.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.render.RenderUtils;
import antileak.base.client.modules.impl.render.Removals;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void elysium$hideTotemAnimation(ItemStack stack, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null || stack == null || !stack.isOf(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isTotemAnimationDisabled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
    )
    private void elysium$captureBlurBeforeHud(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        RenderUtils.beginLiquidBlurFrame();
    }
}
