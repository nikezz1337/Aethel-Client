package antileak.base.mixin;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import antileak.base.client.modules.impl.render.SatelliteFeatureRenderer;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void elysium$addShoulderPetFeature(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> rendererContext =
                (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this;

        ((LivingEntityRendererAccessor) this).elysium$addFeature(new SatelliteFeatureRenderer(rendererContext, context));
    }
}
