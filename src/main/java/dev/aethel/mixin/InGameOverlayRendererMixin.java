package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.module.list.player.NoPush;
import dev.aethel.module.list.render.Removals;
import dev.aethel.util.base.Instance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Оверлей в блоке")) {
            ci.cancel();
            return;
        }

        var noPush = Instance.get(NoPush.class);
        if (noPush != null && noPush.isEnabled() && noPush.collisionList.getValue("Блоки")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Огонь")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
    }
}
