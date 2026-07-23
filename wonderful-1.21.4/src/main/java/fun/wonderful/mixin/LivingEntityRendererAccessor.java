package fun.wonderful.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {

    @Invoker("addFeature")
    boolean wonderful$addFeature(FeatureRenderer<?, ?> feature);

    @Invoker("setupTransforms")
    void wonderful$setupTransforms(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw, float baseScale);

    @Invoker("scale")
    void wonderful$scale(LivingEntityRenderState state, MatrixStack matrices);
}
