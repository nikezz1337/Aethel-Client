package dev.ethereal.inject.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.render.EntityColorEvent;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import dev.ethereal.api.utils.rotation.manager.RotationPlan;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @ModifyExpressionValue(method = "updateRenderState*", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float updateVisalPitch(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != SharedClass.player()) {
            return original;
        }

        RotationManager rotationManager = RotationManager.getInstance();
        RotationComponent rotationComponent = RotationComponent.getInstance();
        if (rotationManager.isEmpty() && rotationComponent.hasAuraRotationTask()) {
            return rotationComponent.getAuraVisualPitch();
        }

        Rotation rotation = rotationManager.getRotation();
        Rotation rotationPrev = rotationManager.getPreviousRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return original;
        }

        return MathHelper.lerpAngleDegrees(tickDelta, rotationPrev.getPitch(), rotation.getPitch());
    }

    @Shadow
    @Nullable
    protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;" + "Lnet/minecraft/client/util/math/MatrixStack;" + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;" + "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)" + "Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        if (!translucent && state.width == 0.6F) {
            int defaultColor = -1;
            EntityColorEvent eventData = new EntityColorEvent(defaultColor);
            Events.post(eventData);
            if (eventData.isCancelled() && eventData.color() != defaultColor) {
                translucent = true;
            }
        }

        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l, @Local(ordinal = 0, argsOnly = true) LivingEntityRenderState renderState) {
        EntityColorEvent event = new EntityColorEvent(l);
        if (renderState.invisibleToPlayer) Events.post(event);
        instance.render(matrixStack, vertexConsumer, i, j, event.color());
    }
}
