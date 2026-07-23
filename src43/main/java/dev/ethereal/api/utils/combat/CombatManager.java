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
import dev.ethereal.api.utils.combat.TpsCalculator;
import dev.ethereal.client.features.modules.movement.SprintModule;

import java.util.Random;

@Getter
@Accessors(fluent = true, chain = true)
public class CombatManager implements QuickImports {

    private final ClickScheduler clickScheduler = new ClickScheduler();
    private final SprintManager sprintManager = new SprintManager(SprintManager.SprintType.LEGIT);
    private final ShieldBreakManager shieldBreakManager = new ShieldBreakManager();

    private boolean dynamicCd = false;
    public static int rayTick;

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

    private float getCooldownThreshold() {
        if (dynamicCd) {
            return mc.player.getAttackCooldownProgressPerTick() < 0.05f
                    ? MathUtil.random(0.9f, 0.994f)
                    : MathUtil.random(0.89f, 0.93f);
        } else if (configurable != null && configurable.tpsSync) {
            float adjust = TpsCalculator.getInstance().getAdjustTicks();
            return Math.max(0.85f, 0.9f - adjust);
        }
        return 0.9f;
    }

    public void handleAttack() {
        dynamicCd = PlayerUtil.isHW() || PlayerUtil.isFT() || PlayerUtil.isST();

        sprintManager.sprintType = SprintManager.SprintType.LEGIT;

        float cd = mc.player.getAttackCooldownProgress(0.5f);
        float cooldownThreshold = getCooldownThreshold();
        float nextCd = cd + mc.player.getAttackCooldownProgressPerTick();

        if (cd < cooldownThreshold && nextCd >= cooldownThreshold) {
            if (mc.player.isSprinting() && shouldPreventSprinting()) {
                mc.player.setSprinting(false);
                //        mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }

        if (!canAttack()) return;

        if (configurable.raytrace) {
            EntityHitResult hitResult = RaytracingUtil.raytraceEntity(configurable.distance, configurable.rotation, configurable.ignoreWalls);
            if (hitResult == null )  {
                rayTick = 0;
                return;
            }
        }

        rayTick++;

        if (mc.player.isBlocking() && configurable.alwaysShield) {
            mc.interactionManager.stopUsingItem(mc.player);
        }

        if (PlayerUtil.isST() && rayTick == 0) {
            return;
        }

        mc.interactionManager.attackEntity(mc.player, configurable.target);
        mc.player.swingHand(Hand.MAIN_HAND);
        //      print(String.valueOf(mc.player.fallDistance));

        clickScheduler.recalculateAfterAttack();
    }

    public boolean canAttack() {
        if (configurable == null) return false;
        if (configurable.noAttackIfEat && PlayerUtil.isEating()) return false;

        if (configurable.shieldBreak && configurable.target instanceof PlayerEntity p &&
                shieldBreakManager.shouldBreakShield(p)) {
            return true;
        }

        float cd = mc.player.getAttackCooldownProgress(0.5f);
        float cooldownThreshold = getCooldownThreshold();

        if (cd < cooldownThreshold) return false;
        if (mc.player.getAttackCooldownProgressPerTick() <= 0) return false;

        if (!configurable.onlyCrits) return true;

        if (configurable.smartCrits && mc.player.isOnGround()) return true;

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

        float minFall = (PlayerUtil.isHW() || PlayerUtil.isFT() || PlayerUtil.isST()) ? MathUtil.random(0.02f, 0.08f) : 0.0f;
        return mc.player.fallDistance > minFall && mc.player.getVelocity().y < 0.0 || mc.player.isOnGround() && configurable.smartCrits;
    }

    private boolean shouldCancelCrit() {
        if (mc.player.isGliding()) return false;
        return mc.player.getAbilities().flying || mc.player.hasNoGravity();
    }

    public boolean shouldPreventSprinting() {
        if (configurable == null) return false;
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