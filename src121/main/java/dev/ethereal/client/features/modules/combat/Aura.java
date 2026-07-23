package dev.ethereal.client.features.modules.combat;

import com.google.common.eventbus.Subscribe;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.client.features.modules.combat.aura.AuraUtil;
import dev.ethereal.client.features.modules.combat.aura.RayTraceUtil;
import dev.ethereal.client.features.modules.combat.aura.UAttack;
import dev.ethereal.client.features.modules.combat.aura.URotate;
import dev.ethereal.client.features.modules.combat.aura.rotation.FreeLookComponent;
import dev.ethereal.client.features.modules.combat.aura.rotation.URotations;
import dev.ethereal.client.features.modules.combat.aura.util.Mathf;
import dev.ethereal.client.features.modules.combat.aura.util.time.TimerUtil;
import lombok.Getter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "AuraTest", category = Category.COMBAT)
public class Aura extends Module {
    @Getter private static final Aura instance = new Aura();

    private final SliderSetting attackRange =
            new SliderSetting("Радиус атаки").value(3f).range(2.5f, 5f).step(0.1f);

    private final SliderSetting preRange =
            new SliderSetting("Радиус обнаружения").value(1f).range(0f, 2f).step(0.1f);

    public final ModeSetting typeRotation =
            new ModeSetting("Тип навидения").value("SpookyTime").values("SpookyTime", "FunTime", "HolyWorld", "Matrix", "Snap");

    public final MultiBooleanSetting targets =
            new MultiBooleanSetting("Кого атаковать").value(
                    new BooleanSetting("Игроков").value(true),
                    new BooleanSetting("Голых").value(true),
                    new BooleanSetting("Мобов").value(false),
                    new BooleanSetting("Друзей").value(false));

    public final ModeSetting attackDelay =
            new ModeSetting("Кулдаун атаки").value("Быстрый").values("Статичный", "Быстрый", "Динамичный", "1.8");

    public final MultiBooleanSetting check =
            new MultiBooleanSetting("Проверки").value(
                    new BooleanSetting("Бить через блоки").value(false),
                    new BooleanSetting("Бить только оружием").value(false),
                    new BooleanSetting("Не бить если кушаеш").value(true),
                    new BooleanSetting("Не атакавать в контейнере").value(false),
                    new BooleanSetting("Автоматичиски ломать щит").value(false),
                    new BooleanSetting("Отжимать щит при ударе").value(false));

    public final ModeSetting motion =
            new ModeSetting("Коррекция движения").value("Свободная").values("Сильная", "Свободная", "Преследование");

    public final BooleanSetting behindTarget =
            new BooleanSetting("Заходить за спину").value(false);

    public final BooleanSetting onlyCrits =
            new BooleanSetting("Умные криты").value(false);

    public final ModeSetting sprint =
            new ModeSetting("Сброс спринта").value("Легит").values("ХвХ", "Легит");

    public static LivingEntity target = null;
    public static LivingEntity lastTarget;

    public long lastLookUpTime = 0;
    public long nextLookUpDelay = ThreadLocalRandom.current().nextLong(90000, 180000);
    public boolean isLookingUp = false;
    public long lookUpStartTime = 0;
    public int lookUpDuration = 0;

    private float[] currentRotations = new float[2];

    int ps = 0;

    private final URotations uRotations = new URotations();

    public Aura() {
        addSettings(attackRange, preRange, typeRotation, targets, attackDelay, check, onlyCrits, behindTarget, motion, sprint);
        new URotations();
        new FreeLookComponent();
    }

    @Override
    public void toggle() {
        super.toggle();
        reset();
    }

    @EventHandler
    public void onEvent(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isAlive()) {
            toggle();
            return;
        }

        if (target == null || !isValidTarget(target)) {
            updateTarget();
        }

        if (target != null && mc.player != null && mc.world != null) {
            lastTarget = target;
            rotation();

            if (!checkToAttack()) {
                attack();
            }
        } else {
            reset();
        }
    }

    @Override
    public void onDisable() {
        target = null;
        super.onDisable();
    }

    @Override
    public void onEnable() {
        target = null;
        super.onEnable();
    }

    @EventHandler
    public void onEvent(MovementInputEvent event) {
        if (UAttack.resetSprintTick(target, ranges())) {
            event.movementSideways = 0.0F;
            event.movementForward = 0.0F;
        }
    }

    public void attack() {
        if (AuraUtil.getStrictDistance(target) >= attackRange.getValue()) {
            return;
        }

        float[] ranges = ranges();
        ranges = new float[]{ranges[0], ranges[1], ranges[0] + ranges[1]};

        boolean canAttack = UAttack.shouldAttack(target, isRay(), attackDelay.is("1.8"), !attackDelay.is("1.8"), 0L, ranges);

        if (!canAttack) return;

        final Runnable[] shieldBreak = UAttack.hitShieldBreakTaskForUse(target, check.isEnabled("Автоматичиски ломать щит")),
                shieldPressBypass = UAttack.resetShieldSilentTaskForUse(true),
                skipSilentSprint = UAttack.skipSilentSprintingTaskForUse(typeRotation.is("HvH"));

        final Runnable preHitSendCodeSingleTick = () -> {
            skipSilentSprint[0].run();
            shieldPressBypass[0].run();
            shieldBreak[0].run();
        }, postHitSendCodeSingleTick = () -> {
            shieldBreak[1].run();
            shieldPressBypass[1].run();
            skipSilentSprint[1].run();
        };

        if (mc.player.isBlocking() && check.isEnabled("Отжимать щит при ударе")) {
            mc.player.stopUsingItem();
        }

        UAttack.useEntity(target, preHitSendCodeSingleTick, postHitSendCodeSingleTick, Hand.MAIN_HAND, true);
        ps = (ps + 1) % 2;
    }

    private boolean checkToAttack() {
        return (mc.player.isUsingItem() && (check.isEnabled("Не бить если кушаеш") && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)))
                || mc.currentScreen != null && check.isEnabled("Не атакавать в контейнере")
                || (!(mc.player.getMainHandStack().getItem() instanceof AxeItem)
                && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)
                && check.isEnabled("Бить только оружием"));
    }

    public float[] ranges() {
        return new float[]{attackRange.getValue(), preRange.getValue()};
    }

    public boolean isRay() {
        return !typeRotation.is("HvH");
    }

    public final TimerUtil tim2 = new TimerUtil();
    public final TimerUtil tim3 = new TimerUtil();

    public void rotation() {
        if (target != null) {
            float[] ranges = ranges();
            ranges = new float[]{ranges[0], ranges[1], ranges[0] + ranges[1]};

            boolean canAttack = UAttack.shouldAttack(target, false, true, true, 0L, ranges);
            boolean inAttackRange = AuraUtil.getStrictDistance(target) < attackRange.getValue();

            switch (typeRotation.getValue()) {
                case "SpookyTime" -> URotate.onSpookyRotation(target, canAttack, inAttackRange);
                case "Matrix" -> URotate.onMatrixRotation(target, canAttack);
                case "HolyWorld" -> URotate.onHolyRotation(target, canAttack);
                case "FunTime" -> URotate.onFunTimeRotation(target, canAttack,
                        attackRange.getValue(), checkToAttack());
                case "Snap" -> URotate.onSnapRotation(target, canAttack);
            }
        }
    }

    private void updateTarget() {
        ArrayList<LivingEntity> validTargets = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity && isValidTarget(livingEntity)) {
                validTargets.add(livingEntity);
            }
        }

        if (validTargets.isEmpty()) {
            target = null;
        } else if (validTargets.size() == 1) {
            target = validTargets.get(0);
        } else {
            validTargets.sort((entity1, entity2) -> {
                Vec3d eyePos = mc.player.getEyePos();
                Vec3d lookVec = mc.player.getRotationVecClient().normalize();

                Vec3d pos1 = entity1.getPos().add(0.0F, entity1.getHeight() / 2.0F, 0.0F).subtract(eyePos).normalize();
                Vec3d pos2 = entity2.getPos().add(0.0F, entity2.getHeight() / 2.0F, 0.0F).subtract(eyePos).normalize();

                double dot1 = lookVec.dotProduct(pos1);
                double dot2 = lookVec.dotProduct(pos2);

                return Double.compare(dot2, dot1);
            });

            target = validTargets.get(0);
        }
    }

    public boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof ClientPlayerEntity || entity.age < 3) {
            return false;
        }

        if (mc.player.distanceTo(entity) > attackRange.getValue() + preRange.getValue()) {
            return false;
        }

        if (!check.isEnabled("Бить через блоки") && !RayTraceUtil.canSeen(RayTraceUtil.getPoint(entity))) {
            return false;
        }

        if (entity instanceof PlayerEntity player) {
            if (entity instanceof ArmorStandEntity) {
                return false;
            }

            if (!targets.isEnabled("Игроков")) {
                return false;
            }

            if (player.getArmor() == 0 && !targets.isEnabled("Голых")) {
                return false;
            }
        }

        if ((entity instanceof Monster || entity instanceof SlimeEntity
                || entity instanceof VillagerEntity
                || entity instanceof DolphinEntity
                || entity instanceof SquidEntity
                || entity instanceof FishEntity || entity instanceof AnimalEntity
                || entity instanceof GhastEntity || entity instanceof ShulkerEntity
                || entity instanceof PhantomEntity || entity instanceof WanderingTraderEntity)
                && !targets.isEnabled("Мобов")) {
            return false;
        }

        return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStandEntity);
    }

    public float cooldownFromLastSwing() {
        return MathHelper.clamp(mc.player.getItemUseTime() / randomLerp(8, 12), 0.0F, 1.0F);
    }

    public void reset() {
        target = null;
        if (mc.player != null) {
            isLookingUp = false;
            lookUpStartTime = 0;
        }
    }

    public float randomLerp(float min, float max) {
        return Mathf.lerp(max, min, new SecureRandom().nextFloat());
    }

    public LivingEntity getTarget() {
        return target;
    }

    public float[] getCurrentRotations() {
        if (mc.player != null) {
            currentRotations[0] = mc.player.getYaw();
            currentRotations[1] = mc.player.getPitch();
        }
        return currentRotations;
    }

    public double getRange() {
        return attackRange.getValue();
    }

    public boolean isMoveCorrection() {
        return motion.getValue().equals("Сильная") || motion.getValue().equals("Преследование");
    }

    public boolean isSilentCorrection() {
        return motion.getValue().equals("Сильная");
    }

    public boolean shouldSuppressSprintForCriticalWindow() {
        return false;
    }
}
