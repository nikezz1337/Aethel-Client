package dev.aethel.module.list.combat;

import com.google.common.eventbus.Subscribe;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventTick;
import dev.aethel.event.list.WorldChangeEvent;
import dev.aethel.event.list.WorldLoadEvent;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.aura.*;
import dev.aethel.module.list.combat.aura.util.Mathf;
import dev.aethel.module.list.combat.aura.util.time.TimerUtil;
import dev.aethel.module.list.movement.AirStuck;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(
        moduleName = "Aura",
        moduleCategory = ModuleCategory.COMBAT,
        moduleDesc = "Автоматически наводится и атакует выбранные цели"
)
public class Aura extends Module {

    private static Aura instance;

    public static Aura getInstance() {
        return instance;
    }

    public Aura() {
        instance = this;
    }

    private final SliderSetting attackRange =
            new SliderSetting("Радиус атаки", 3.0D, 2.5D, 5.0D, 0.1D);

    private final SliderSetting preRange =
            new SliderSetting("Радиус обнаружения", 1.0D, 0.0D, 3.0D, 0.1D);

    public final ModeSetting typeRotation =
            new ModeSetting("Тип навидения", "SpookyTime", "SpookyTime", "FunTime", "HolyWorld", "Matrix", "Snap");

    public final dev.aethel.module.settings.MultiBooleanSetting targets =
            new dev.aethel.module.settings.MultiBooleanSetting("Кого атаковать", "",
                    new BooleanSetting("Игроков", true),
                    new BooleanSetting("Голых", true),
                    new BooleanSetting("Мобов", false),
                    new BooleanSetting("Друзей", false));

    public final ModeSetting attackDelay =
            new ModeSetting("Кулдаун атаки", "Статичный", "Статичный", "Быстрый", "Динамичный", "1.8");

    public final dev.aethel.module.settings.MultiBooleanSetting check =
            new dev.aethel.module.settings.MultiBooleanSetting("Проверки", "",
                    new BooleanSetting("Бить через блоки", false),
                    new BooleanSetting("Бить только оружием", false),
                    new BooleanSetting("Не бить если кушаеш", true),
                    new BooleanSetting("Не атакавать в контейнере", false),
                    new BooleanSetting("Автоматичиски ломать щит", false),
                    new BooleanSetting("Отжимать щит при ударе", false),
                    new BooleanSetting("Swap голову Ареса", false));

    public final ModeSetting motion =
            new ModeSetting("Коррекция движения", "Свободная", "Сильная", "Свободная", "Преследование");

    public final BooleanSetting onlyCrits =
            new BooleanSetting("Умные криты", false);

    public final BooleanSetting behindTarget =
            new BooleanSetting("Заходить за спину", false);

    public static LivingEntity target = null;
    public static LivingEntity lastTarget;
    public final TimerUtil timerUtil = new TimerUtil();

    float tick = 0;
    int ps = 0;

    public long lastLookUpTime = 0;
    public long nextLookUpDelay = ThreadLocalRandom.current().nextLong(90000, 180000);
    public boolean isLookingUp = false;
    public long lookUpStartTime = 0;
    public int lookUpDuration = 0;

    private final dev.aethel.module.list.combat.aura.rotation.URotations uRotations = new dev.aethel.module.list.combat.aura.rotation.URotations();

    public SliderSetting attackRange() { return attackRange; }
    public SliderSetting preRange() { return preRange; }
    public ModeSetting motion() { return motion; }

    @Override
    public void toggle() {
        super.toggle();
        reset();
    }

    @Subscribe
    public void onEvent(EventTick event) {
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

    @Subscribe
    public void onEvent(WorldChangeEvent event) {
        reset();
    }

    @Subscribe
    public void onEvent(WorldLoadEvent event) {
        reset();
    }

    public void attack() {
        if (AuraUtil.getStrictDistance(target) >= attackRange.getFloatValue()) {
            return;
        }

        float[] ranges = ranges();
        ranges = new float[]{ranges[0], ranges[1], ranges[0] + ranges[1]};

        boolean canAttack = UAttack.shouldAttack(target, isRay(), attackDelay.is("1.8"), !attackDelay.is("1.8"), 0L, ranges);

        if (!canAttack) return;

        final Runnable[] shieldBreak = UAttack.hitShieldBreakTaskForUse(target, check.getValue("Автоматичиски ломать щит")),
                shieldPressBypass = UAttack.resetShieldSilentTaskForUse(true),
                skipSilentSprint = UAttack.skipSilentSprintingTaskForUse(typeRotation.is("HvH"));

        // Ares head swap
        final boolean swapAresHead = check.getValue("Swap голову Ареса");
        final int[] aresHeadSlot = {-1};

        final Runnable preHitSendCodeSingleTick = () -> {
            skipSilentSprint[0].run();
            shieldPressBypass[0].run();
            shieldBreak[0].run();
            if (swapAresHead) {
                aresHeadSlot[0] = findAresHead();
//                if (aresHeadSlot[0] != -1) {
//                    dev.aethel.util.player.other.InventoryUtil.swapBypass(aresHeadSlot[0]);
//                }
            }
        }, postHitSendCodeSingleTick = () -> {
            shieldBreak[1].run();
            shieldPressBypass[1].run();
            skipSilentSprint[1].run();
//            if (swapAresHead && aresHeadSlot[0] != -1) {
//                dev.aethel.util.player.other.InventoryUtil.swapBypass(aresHeadSlot[0]);
//            }
        };

        if (mc.player.isBlocking() && check.getValue("Отжимать щит при ударе")) {
            mc.player.stopUsingItem();
        }

        UAttack.useEntity(target, preHitSendCodeSingleTick, postHitSendCodeSingleTick, Hand.MAIN_HAND, true);
        ps = (ps + 1) % 2;
    }

    private int findAresHead() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() != Items.PLAYER_HEAD) continue;
            if (stack.getName().getString().toLowerCase(java.util.Locale.ROOT).contains("ареса")) return i;
        }
        return -1;
    }

    private boolean checkToAttack() {
        if (isAirStuckBypass()) return false;
        return (mc.player.isUsingItem() && (check.getValue("Не бить если кушаеш") && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)))
                || mc.currentScreen != null && check.getValue("Не атакавать в контейнере")
                || (!(mc.player.getMainHandStack().getItem() instanceof AxeItem)
                && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)
                && check.getValue("Бить только оружием"));
    }

    public float[] ranges() {
        return new float[]{attackRange.getFloatValue(), preRange.getFloatValue()};
    }

    public boolean isRay() {
        return !typeRotation.is("HvH");
    }

    private boolean isAirStuckBypass() {
        AirStuck airStuck = Aethel.getInstance().getModuleStorage().get(AirStuck.class);
        return airStuck != null && airStuck.isEnabled();
    }

    public final TimerUtil tim2 = new TimerUtil();
    public final TimerUtil tim3 = new TimerUtil();

    public void rotation() {
        if (target != null) {
            float[] ranges = ranges();
            ranges = new float[]{ranges[0], ranges[1], ranges[0] + ranges[1]};

            boolean canAttack = isAirStuckBypass() || UAttack.shouldAttack(target, false, true, true, 0L, ranges);
            boolean inAttackRange = AuraUtil.getStrictDistance(target) < attackRange.getFloatValue();

            switch (typeRotation.getValue()) {
                case "SpookyTime" -> URotate.onSpookyRotation(target, canAttack, inAttackRange);
                case "Matrix" -> URotate.onMatrixRotation(target, canAttack);
                case "HolyWorld" -> URotate.onHolyRotation(target, ranges);
                case "FunTime" -> URotate.onFunTimeRotation(target, ranges);
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

        if (mc.player.distanceTo(entity) > attackRange.getFloatValue() + preRange.getFloatValue()) {
            return false;
        }

        if (!isAirStuckBypass()) {
            if (!check.getValue("Бить через блоки") && !RayTraceUtil.canSeen(RayTraceUtil.getPoint(entity))) {
                return false;
            }

            if (entity instanceof PlayerEntity player) {
                if (entity instanceof ArmorStandEntity) {
                    return false;
                }

                if (!targets.getValue("Игроков")) {
                    return false;
                }

                if (player.getArmor() == 0 && !targets.getValue("Голых")) {
                    return false;
                }

                if (!targets.getValue("Друзей") && dev.aethel.config.FriendManager.isFriend(player.getName().getString())) {
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
                    && !targets.getValue("Мобов")) {
                return false;
            }
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
}
