package dev.aethel.module.list.combat;

import com.google.common.eventbus.Subscribe;
import dev.aethel.config.FriendManager;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.aura.rotation.Rotation;
import dev.aethel.module.list.combat.aura.rotation.URotations;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(
    moduleName = "ProjectileHelper",
    moduleCategory = ModuleCategory.COMBAT,
    moduleDesc = "Помощник по метательным предметам"
)
public class ProjectileHelper extends Module {

    private static ProjectileHelper instance;

    public static ProjectileHelper getInstance() { return instance; }
    public LivingEntity getTarget() { return target; }

    private final MultiBooleanSetting gun = new MultiBooleanSetting("Учитывать", "",
            new BooleanSetting("Арбалет", true),
            new BooleanSetting("Трезубец", true),
            new BooleanSetting("Зелья", true),
            new BooleanSetting("Лук", true)
    );

    private final BooleanSetting autoShoot = new BooleanSetting("Авто-выстрел", true);
    private final SliderSetting predict = new SliderSetting("Сила предикта", 1.03, 1.0, 3.0, 0.01);
    private final SliderSetting attackRange = new SliderSetting("Дистанция", 50.0, 1.0, 100.0, 1.0);
    private final SliderSetting aimSpeed = new SliderSetting("Скорость наводки", 36.0, 10.0, 80.0, 1.0);

    private LivingEntity target = null;
    private int shootDelay = 0;

    public ProjectileHelper() {
        instance = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        URotations.resetParentTimeout();
        target = null;
        shootDelay = 0;
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (shootDelay > 0) {
            shootDelay--;
            return;
        }

        boolean isCrossbow = mc.player.getMainHandStack().getItem() == Items.CROSSBOW && gun.getValue("Арбалет");
        boolean isTrident = mc.player.getMainHandStack().getItem() == Items.TRIDENT && gun.getValue("Трезубец");
        boolean isBow = mc.player.getMainHandStack().getItem() == Items.BOW && gun.getValue("Лук");
        boolean isPotion = (mc.player.getMainHandStack().getItem() == Items.SPLASH_POTION
                || mc.player.getMainHandStack().getItem() == Items.LINGERING_POTION) && gun.getValue("Зелья");

        if (!isCrossbow && !isTrident && !isBow && !isPotion) {
            if (target != null) {
                URotations.resetParentTimeout();
            }
            target = null;
            return;
        }

        if (isCrossbow) {
            if (!mc.player.isUsingItem() && !CrossbowItem.isCharged(mc.player.getMainHandStack())) {
                if (target != null) {
                    URotations.resetParentTimeout();
                }
                target = null;
                return;
            }
        }

        if (isBow || isTrident || isPotion) {
            if (!mc.player.isUsingItem()) {
                if (target != null) {
                    URotations.resetParentTimeout();
                }
                target = null;
                return;
            }
        }

        target = findTarget();
        if (target == null || !target.isAlive()) {
            if (this.target != null) {
                URotations.resetParentTimeout();
            }
            target = null;
            return;
        }

        float[] rotation = calculateRotation(target);

        float speed = (float) aimSpeed.getFloatValue();
        URotations.update(new Rotation(rotation[0], rotation[1]), speed, speed, 40, 100);

        boolean reached = Math.abs(MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw())) < 2
                && Math.abs(rotation[1] - mc.player.getPitch()) < 2;

        if (reached && autoShoot.getValue()) {
            if (isCrossbow && CrossbowItem.isCharged(mc.player.getMainHandStack())) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                shootDelay = 4;
            } else if (isBow || isTrident || isPotion) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                shootDelay = 4;
            }
        }
    }

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDist = attackRange.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player && entity != mc.player) {
                if (FriendManager.isFriend(player.getName().getString())) continue;
                if (!player.isAlive()) continue;

                double dist = player.getEyePos().distanceTo(mc.player.getEyePos());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = player;
                }
            }
        }

        return best;
    }

    private float[] calculateRotation(LivingEntity targetEntity) {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d targetPos = targetEntity.getPos().add(0, targetEntity.getHeight() * 0.4, 0);

        boolean isCrossbow = mc.player.getMainHandStack().getItem() == Items.CROSSBOW;
        boolean isTrident = mc.player.getMainHandStack().getItem() == Items.TRIDENT;
        boolean isBow = mc.player.getMainHandStack().getItem() == Items.BOW;

        double gravity, velocity;
        if (isCrossbow) { velocity = 3.15; gravity = 0.05; }
        else if (isTrident) { velocity = 3.5; gravity = 0.03; }
        else if (isBow) { velocity = 3.0; gravity = 0.05; }
        else { velocity = 0.5; gravity = 0.05; }

        double velocityX = targetEntity.getX() - targetEntity.prevX;
        double velocityY = targetEntity.getY() - targetEntity.prevY;
        double velocityZ = targetEntity.getZ() - targetEntity.prevZ;

        double deltaX = targetPos.x - playerPos.x;
        double deltaY = targetPos.y - playerPos.y;
        double deltaZ = targetPos.z - playerPos.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double flightTime = horizontalDistance / (velocity * 0.8);

        Vec3d predictedPos = new Vec3d(
                targetPos.x + velocityX * flightTime * predict.getFloatValue(),
                targetPos.y + velocityY * Math.min(flightTime * 1.5, 0.5),
                targetPos.z + velocityZ * flightTime * predict.getFloatValue()
        );

        double predDeltaX = predictedPos.x - playerPos.x;
        double predDeltaY = predictedPos.y - playerPos.y;
        double predDeltaZ = predictedPos.z - playerPos.z;
        double predHorizontalDistance = Math.sqrt(predDeltaX * predDeltaX + predDeltaZ * predDeltaZ);

        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(predDeltaZ, predDeltaX)) - 90.0F);

        double discriminant = Math.pow(velocity, 4) - gravity * (gravity * predHorizontalDistance * predHorizontalDistance + 2 * predDeltaY * velocity * velocity);
        double optimalPitch;
        if (discriminant >= 0) {
            optimalPitch = -Math.toDegrees(Math.atan(
                    (velocity * velocity - Math.sqrt(discriminant)) / (gravity * predHorizontalDistance)
            ));
        } else {
            optimalPitch = -Math.toDegrees(Math.atan2(predDeltaY, predHorizontalDistance));
        }

        if (Double.isNaN(optimalPitch) || Double.isInfinite(optimalPitch)) {
            optimalPitch = -Math.toDegrees(Math.atan2(predDeltaY, predHorizontalDistance));
        }

        return new float[]{targetYaw, (float) optimalPitch};
    }
}
