package dev.ethereal.client.features.modules.combat;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.move.VelocityEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.configs.FriendManager;
import dev.ethereal.api.utils.combat.PredictUtils;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleRegister(name = "Aim Bot", category = Category.COMBAT)
public class AimBotModule extends Module {
    @Getter private static final AimBotModule instance = new AimBotModule();

    private final MultiBooleanSetting items = new MultiBooleanSetting("Оружие").value(
            new BooleanSetting("Лук").value(true),
            new BooleanSetting("Арбалет").value(true),
            new BooleanSetting("Трезубец").value(true)
    );

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Таргеты").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Мобы").value(true),
            new BooleanSetting("Животные").value(false),
            new BooleanSetting("Друзья").value(false)
    );

    private final SliderSetting distance        = new SliderSetting("Дистанция").value(30f).range(5f, 100f).step(1f);
    private final SliderSetting fov             = new SliderSetting("FOV").value(90f).range(1f, 180f).step(1f);
    private final SliderSetting speed           = new SliderSetting("Скорость").value(10f).range(1f, 180f).step(1f);
    private final BooleanSetting prediction     = new BooleanSetting("Предсказание").value(true);
    private final BooleanSetting moveCorrection = new BooleanSetting("Коррекция движения").value(true);
    private final BooleanSetting silent         = new BooleanSetting("Силент").value(true);

    private LivingEntity currentTarget = null;

    // Кешируем скорость трезубца из реальной сущности в мире
    private double cachedTridentSpeed = 0.5;

    public AimBotModule() {
        addSettings(items, targets, distance, fov, speed, prediction, moveCorrection, silent);
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        PredictUtils.clear();
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.world == null) return;
        for (var entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && living != mc.player) {
                PredictUtils.updateEntity(living);
            }
            // Ловим трезубец игрока сразу после броска — берём его реальную скорость
            if (entity instanceof net.minecraft.entity.projectile.TridentEntity trident) {
                var ownerUuid = trident.getOwner();
                if (ownerUuid != null && ownerUuid == mc.player) {
                    double vel = trident.getVelocity().length();
                    if (vel > 0.5) cachedTridentSpeed = vel;
                }
            }
        }
        PredictUtils.cleanup();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!isHoldingSelected()) {
            currentTarget = null;
            return;
        }

        currentTarget = findTarget();
        if (currentTarget == null) return;

        Vec3d aimPos = getAimPos(currentTarget);
        Rotation rotation = RotationUtil.rotationAt(aimPos);

        float yawDelta = Math.abs(MathHelper.wrapDegrees(rotation.getYaw() - mc.player.getYaw()));
        if (yawDelta > fov.getValue()) return;

        float spd = speed.getValue();
        RotationComponent.update(rotation, spd, spd, spd, spd, 2, 10, !silent.getValue());
    }

    // Коррекция движения — направляем движение по ротации аима
    @EventHandler
    public void onVelocity(VelocityEvent event) {
        if (!moveCorrection.getValue()) return;
        if (currentTarget == null) return;
        Rotation rot = RotationComponent.getInstance().currentRotation();
        if (rot == null) return;
        event.setVelocity(net.minecraft.entity.Entity.movementInputToVelocity(
                event.getMovementInput(), event.getSpeed(), rot.getYaw()));
    }

    private boolean isHoldingSelected() {
        var item = mc.player.getMainHandStack().getItem();
        if (item == Items.BOW      && items.isEnabled("Лук"))     return mc.player.isUsingItem();
        if (item == Items.CROSSBOW && items.isEnabled("Арбалет")) return CrossbowItem.isCharged(mc.player.getMainHandStack());
        if (item == Items.TRIDENT  && items.isEnabled("Трезубец")) return mc.player.isUsingItem();
        return false;
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.getHealth() <= 0) continue;
            if (!isValidTarget(living)) continue;

            double dist = mc.player.getEyePos().distanceTo(RotationUtil.getSpot(living));
            if (dist > distance.getValue()) continue;

            if (dist < bestDist) {
                bestDist = dist;
                best = living;
            }
        }
        return best;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            boolean isFriend = FriendManager.getInstance().contains(player.getName().getString());
            if (isFriend) return targets.isEnabled("Друзья");
            return targets.isEnabled("Игроки");
        }
        if (entity instanceof AnimalEntity) return targets.isEnabled("Животные");
        if (entity instanceof MobEntity)    return targets.isEnabled("Мобы");
        return false;
    }

    private Vec3d getAimPos(LivingEntity target) {
        Vec3d basePos = target.getPos().add(0, 0, 0);

        double dist = mc.player.getEyePos().distanceTo(basePos);
        double projSpeed = getProjectileSpeed();
        int ticks = Math.max(1, (int) Math.round(dist / projSpeed));

        // Компенсация гравитации: 0.5 * 0.05 * ticks²
        double gravityCompensation = 0.3 * 0.05 * ticks * ticks;

        if (!prediction.getValue()) {
            return basePos.add(0, gravityCompensation, 0);
        }

        PredictUtils.PositionData data = PredictUtils.getData(target);
        if (data == null || !data.isMoving()) {
            return basePos.add(0, gravityCompensation, 0);
        }

        Vec3d predicted = PredictUtils.predict(target, ticks);
        double minY = target.getPos().y;
        return new Vec3d(predicted.x, Math.max(predicted.y + gravityCompensation, minY), predicted.z);
    }

    /**
     * Реальная скорость снаряда в блоках/тик из ванильных значений МС:
     * - Трезубец: 2.5 (хардкод в TridentItem)
     * - Арбалет:  3.15 (хардкод в CrossbowItem)
     * - Лук:      3.0 * charge, где charge = min(drawTicks / 20, 1)
     */
    private double getProjectileSpeed() {
        var item = mc.player.getMainHandStack().getItem();
        if (item == Items.TRIDENT) return 1.2;
        if (item == Items.CROSSBOW) return 3.15;
        if (item == Items.BOW) {
            int drawTicks = mc.player.getItemUseTime();
            float charge = Math.min(drawTicks / 20f, 1f);
            return 3.0 * charge;
        }
        return 2.5;
    }
}
