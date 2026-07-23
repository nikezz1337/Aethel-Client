package fun.wonderful.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import fun.wonderful.client.modules.impl.render.SeeInvisiblesRenderState;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements SeeInvisiblesRenderState {

    @Unique
    private boolean wonderful$seeInvisiblesTarget;

    @Override
    public boolean wonderful$isSeeInvisiblesTarget() {
        return wonderful$seeInvisiblesTarget;
    }

    @Override
    public void wonderful$setSeeInvisiblesTarget(boolean value) {
        wonderful$seeInvisiblesTarget = value;
    }
}
