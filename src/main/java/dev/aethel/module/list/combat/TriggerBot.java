package dev.aethel.module.list.combat;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.aura.UAttack;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

@ModuleInformation(
    moduleName = "TriggerBot",
    moduleCategory = ModuleCategory.COMBAT,
    moduleDesc = "Атака сущностей под прицелом"
)
public class TriggerBot extends Module {

    private final MultiBooleanSetting targetType = new MultiBooleanSetting("Тип таргета", "",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Мобы", true),
            new BooleanSetting("Животные", true),
            new BooleanSetting("Друзья", false),
            new BooleanSetting("Подставка для брони", false)
    );

    private final SliderSetting attackRange = new SliderSetting("Дистанция удара", 3.0, 1.0, 6.0, 0.1);

    private final MultiBooleanSetting attackSetting = new MultiBooleanSetting("Настройки", "",
            new BooleanSetting("Умные криты", true),
            new BooleanSetting("Не бить если ешь", true)
    );

    private boolean attackedThisJump = false;

    @Override
    public void onDisable() {
        super.onDisable();
        attackedThisJump = false;
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (mc.player.isOnGround()) {
            attackedThisJump = false;
        }

        if (!(mc.crosshairTarget instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity living)) return;
        if (!isValidTarget(living)) return;

        float range = attackRange.getFloatValue();
        if (mc.player.distanceTo(living) > range) return;

        if (mc.player.isUsingItem() && attackSetting.getValue("Не бить если ешь")) return;

        if (attackSetting.getValue("Умные криты")) {
            if (!UAttack.isBestMomentToHit(true)) return;
            if (attackedThisJump) return;

            mc.interactionManager.attackEntity(mc.player, living);
            mc.player.swingHand(Hand.MAIN_HAND);
            attackedThisJump = true;
        } else {
            mc.interactionManager.attackEntity(mc.player, living);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive()) return false;

        if (entity instanceof PlayerEntity player) {
            if (!targetType.getValue("Игроки")) return false;
            if (!targetType.getValue("Друзей") && dev.aethel.config.FriendManager.isFriend(player.getName().getString())) return false;
            if (entity instanceof net.minecraft.entity.decoration.ArmorStandEntity && !targetType.getValue("Подставка для брони")) return false;
            return true;
        }

        if (entity instanceof AnimalEntity) {
            return targetType.getValue("Животные");
        }

        return targetType.getValue("Мобы");
    }
}
