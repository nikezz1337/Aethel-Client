package dev.ethereal.api.utils.combat;

import dev.ethereal.api.utils.rotation.manager.Rotation;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.player.move.SprintEvent;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.rotation.RaytracingUtil;
import dev.ethereal.api.utils.rotation.rotations.FunTimeRotation;
import dev.ethereal.api.utils.combat.TpsCalculator;
import dev.ethereal.client.features.modules.movement.SprintModule;

import java.util.Random;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatManager implements QuickImports {

    private static final Random rnd = new Random();

    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final SprintManager sprintManager = new SprintManager(SprintManager.SprintType.LEGIT);
    private final ShieldBreakManager shieldBreakManager = new ShieldBreakManager();

    private boolean isHolyWorld = false;

    public CombatManager() {
        SprintEvent.getInstance().subscribe(new Listener<>(1, this::handleLegitSprint));
    }

    @Getter @Setter
    private CombatExecutor.CombatConfigurable configurable;

    private void handleLegitSprint(SprintEvent.SprintEventData event) {
        if (configurable == null || configurable.target == null) {
            sprintManager.legitSprint(event, false);
            return;
        }
        boolean rule = shouldPreventSprinting();
        sprintManager.legitSprint(event, rule);
    }

    /** Проверка, находимся ли мы на HolyWorld */
    private boolean isOnHolyWorld() {
        if (mc.getCurrentServerEntry() == null) return false;
        String address = mc.getCurrentServerEntry().address.toLowerCase();
        return address.contains("holyworld");
    }


    public void handleAttack() {
        isHolyWorld = PlayerUtil.isHW() || PlayerUtil.isFT();

        sprintManager.sprintType = SprintManager.SprintType.LEGIT;

     //   FunTimeRotation.updateAttackState(canAttack());

        if (!canAttack()) return;

        if (configurable.raytrace) {
            EntityHitResult hitResult = RaytracingUtil.raytraceEntity(configurable.distance, configurable.rotation, configurable.ignoreWalls);
            if (hitResult == null || hitResult.getEntity() != configurable.target) return;
        }

        if (mc.player.isBlocking() && configurable.alwaysShield) {
            mc.interactionManager.stopUsingItem(mc.player);
        }

        mc.interactionManager.attackEntity(mc.player, configurable.target);
        mc.player.swingHand(Hand.MAIN_HAND);
       print(String.valueOf(mc.player.fallDistance));

        clickScheduler.recalculateAfterAttack();

   //     FunTimeRotation.attackCount++;

    }

    public boolean canAttack() {
        if (configurable.noAttackIfEat && PlayerUtil.isEating()) return false;

        if (configurable.shieldBreak && configurable.target instanceof PlayerEntity p &&
                shieldBreakManager.shouldBreakShield(p)) {
            return true;
        }

        float cd = mc.player.getAttackCooldownProgress(0.5f);

        float cooldownThreshold;
        if (isHolyWorld) {
            cooldownThreshold = mc.player.getAttackCooldownProgressPerTick() < 0.05f
                    ? MathUtil.random(0.94f, 0.995f)
                    : MathUtil.random(0.89f, 0.93f);
        } else if (configurable.tpsSync) {
            float adjust = TpsCalculator.getInstance().getAdjustTicks();
            cooldownThreshold = Math.max(0.85f, 0.9f - adjust);
        } else {
            cooldownThreshold = 0.9f;
        }

        if (cd < cooldownThreshold) return false;
        if (mc.player.getAttackCooldownProgressPerTick() <= 0) return false;

        if (!configurable.onlyCrits) return true;

        // Smart crits: allow hitting on ground only when not jumping (velocity.y == 0)
        if (configurable.smartCrits && mc.player.isOnGround() && mc.player.getVelocity().y == 0.0) return true;

        if (shouldCancelCrit()) return false;

        return canPerformCriticalHit(configurable.target);
    }

    public boolean canPerformCriticalHit(Entity target) {
        if (mc.player == null) return false;

        if (mc.player.isGliding()) return true;
        if (mc.player.isClimbing()) return true;
        if (mc.player.isInLava()) return true;
        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) return true;
        if (mc.player.isSwimming()) return true;
        if (mc.player.hasVehicle()) return true;
        if (PlayerUtil.isInWeb()) return true;
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) return true;
        if (mc.player.hasStatusEffect(StatusEffects.LEVITATION)) return true;
        if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) return true;

        if (mc.player.isOnGround()) return false;

        float minFall = (PlayerUtil.isHW() || PlayerUtil.isFT()) ? MathUtil.random(0.02f, 0.08f) : 0.0f;
        return mc.player.fallDistance > minFall && mc.player.getVelocity().y < 0.0;
    }

    private boolean shouldCancelCrit() {
        if (mc.player.isGliding()) return false;
        return mc.player.getAbilities().flying || mc.player.hasNoGravity();
    }

    public boolean shouldPreventSprinting() {
        if (!configurable.onlyCrits) return false;
        if (configurable.smartCrits && mc.player.isOnGround()) return false;
        if (mc.player.isOnGround()) return false;

        return (mc.player.fallDistance > 0.0f && mc.player.getVelocity().y < 0.0) ||
                (mc.player.getVelocity().y <= 0.17477328182606651 &&
                        !mc.player.isClimbing() &&
                        !mc.player.hasVehicle() &&
                        !mc.player.hasStatusEffect(StatusEffects.LEVITATION) &&
                        !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING));
    }
}