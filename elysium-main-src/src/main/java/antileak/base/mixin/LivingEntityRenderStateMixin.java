package antileak.base.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import antileak.base.client.modules.impl.render.SeeInvisiblesRenderState;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements SeeInvisiblesRenderState {

    @Unique
    private boolean elysium$seeInvisiblesTarget;

    @Override
    public boolean elysium$isSeeInvisiblesTarget() {
        return elysium$seeInvisiblesTarget;
    }

    @Override
    public void elysium$setSeeInvisiblesTarget(boolean value) {
        elysium$seeInvisiblesTarget = value;
    }
}
