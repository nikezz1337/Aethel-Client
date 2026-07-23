package antileak.base.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import antileak.base.api.QClient;
import antileak.base.api.events.implement.EventOnMovePost;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.client.modules.impl.player.NoPush;
import antileak.base.client.modules.impl.render.SeeInvisibles;
import antileak.base.client.modules.impl.render.ShaderEsp;

@Mixin(Entity.class)
public abstract class EntityMixin implements QClient {

    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    private boolean fixFallDistanceCalculation(boolean original) {
        if ((Object) this == mc.player) {
            return false;
        }
        return original;
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    public void pushAwayFrom(CallbackInfo ci) {
        if ((Object) this != mc.player || ModuleClass.INSTANCE == null) return;
        NoPush noPush = ModuleClass.noPush;
        if (noPush != null && noPush.isEnable() && noPush.getCollisionList().is("Игроки")) {
            ci.cancel();
        }
    }

    @Inject(method = "isPushedByFluids", at = @At("RETURN"), cancellable = true)
    public void isPushedByFluids(CallbackInfoReturnable<Boolean> ci) {
        if ((Object) this != mc.player || ModuleClass.INSTANCE == null) return;
        NoPush noPush = ModuleClass.INSTANCE.noPush;
        if (noPush != null && noPush.isEnable() && noPush.getCollisionList().is("Вода")) {
            ci.setReturnValue(false);
        }
    }

    @Inject(method = "updateVelocity", at = @At("TAIL"), require = 0)
    private void onVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        Entity me = (Entity) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && me.getId() == client.player.getId()) {
            new EventOnMovePost(speed, movementInput).call();
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void elysium$getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        if (ModuleClass.INSTANCE == null) return;

        ShaderEsp shaderEsp = ModuleClass.INSTANCE.shaderEsp;
        if (shaderEsp != null && shaderEsp.shouldOutline((Entity) (Object) this)) {
            cir.setReturnValue(shaderEsp.getOutlineColor());
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void elysium$allowSeeInvisibles(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof PlayerEntity target) || ModuleClass.INSTANCE == null) {
            return;
        }

        SeeInvisibles seeInvisibles = ModuleClass.seeInvisibles;
        if (seeInvisibles != null && seeInvisibles.shouldRenderInvisible(target)) {
            cir.setReturnValue(false);
        }
    }
}
