package dev.aethel.mixin;

import dev.aethel.module.list.combat.Hitbox;
import dev.aethel.util.base.Instance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class HitboxMixin {

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<Box> cir) {
        Entity self = (Entity) (Object) this;
        if (self == net.minecraft.client.MinecraftClient.getInstance().player) return;

        Hitbox hitbox = Instance.get(Hitbox.class);
        if (hitbox == null || !hitbox.isEnabled()) return;

        if (!isValidTarget(self, hitbox)) return;

        Box original = cir.getReturnValue();
        double expand = hitbox.scale.getValue();
        if (expand <= 0) return;

        cir.setReturnValue(original.expand(expand, expand, expand));
    }

    private boolean isValidTarget(Entity entity, Hitbox hitbox) {
        boolean isPlayer = entity instanceof PlayerEntity;
        boolean isMob = entity instanceof MobEntity && !(entity instanceof AnimalEntity);
        boolean isAnimal = entity instanceof AnimalEntity;
        boolean isInvisible = entity.isInvisible();
        boolean isNaked = isPlayer && isNaked((PlayerEntity) entity);

        if (isPlayer && hitbox.targets.getValue("Игроков")) return true;
        if (isMob && hitbox.targets.getValue("Мобов")) return true;
        if (isAnimal && hitbox.targets.getValue("Животных")) return true;
        if (isInvisible && hitbox.targets.getValue("Невидимых")) return true;
        if (isNaked && hitbox.targets.getValue("Голых")) return true;

        return false;
    }

    private boolean isNaked(PlayerEntity player) {
        return player.getInventory().armor.get(0).isEmpty()
                && player.getInventory().armor.get(1).isEmpty()
                && player.getInventory().armor.get(2).isEmpty()
                && player.getInventory().armor.get(3).isEmpty();
    }
}
