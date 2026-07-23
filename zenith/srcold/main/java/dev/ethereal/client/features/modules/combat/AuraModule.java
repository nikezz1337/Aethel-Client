package dev.ethereal.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.other.RotationUpdateEvent;
import dev.ethereal.api.event.events.player.world.AttackEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.combat.CombatExecutor;
import dev.ethereal.api.utils.combat.TargetManager;
import dev.ethereal.api.utils.rotation.misc.AuraUtil;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import dev.ethereal.api.utils.rotation.manager.RotationMode;
import dev.ethereal.api.utils.rotation.manager.RotationStrategy;
import dev.ethereal.api.utils.rotation.rotations.*;
import dev.ethereal.api.utils.task.TaskPriority;
import dev.ethereal.client.features.modules.combat.elytratarget.ElytraTargetModule;

import java.util.List;
import java.util.Random;

@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    @Getter private static final AuraModule instance = new AuraModule();

    private final TargetManager targetManager = new TargetManager();
    public final CombatExecutor combatExecutor = new CombatExecutor();

    @Getter private final ModeSetting aimMode = new ModeSetting("Ротация").value("Smooth").values(
            "Smooth", "Snap",
            "Funtime", "Lony Grief", "Spooky Time", "HvH"
    );

    private final SliderSetting distance = new SliderSetting("Радиус").value(3f).range(2.5f, 6f).step(0.1f);
    private final SliderSetting preDistance = new SliderSetting("Доп дист").value(0.3f).range(0f, 3f).step(0.1f);
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Таргеты").value(
            new BooleanSetting("Игроки").value(true),
            new BooleanSetting("Мобы").value(true),
            new BooleanSetting("Животные").value(true)
    );

    public final MultiBooleanSetting options = combatExecutor.options();

    private final BooleanSetting clientLook = new BooleanSetting("client look").value(false);
    private final BooleanSetting elytraOverride = new BooleanSetting("Элитра оверрайд").value(false);
    private final SliderSetting elytraDistance = new SliderSetting("Элитра радиус").value(4f).range(2.5f, 6f).step(0.1f).setVisible(elytraOverride::getValue);
    private final SliderSetting elytraPreDistance = new SliderSetting("Элитра доп дист").value(16f).range(0f, 32f).step(0.1f).setVisible(elytraOverride::getValue);

    public final BooleanSetting moveCorrection = new BooleanSetting("Коррекция движения").value(true);
    public final ModeSetting correctionMode = new ModeSetting("Тип").value("Сфокусированная").values("Сфокусированная", "Свободная").setVisible(moveCorrection::getValue);

    public LivingEntity target;

    public AuraModule() {
        addSettings(aimMode, distance, preDistance, targets, options, clientLook,
                elytraOverride, elytraDistance, elytraPreDistance,
                moveCorrection, correctionMode
        );
    }

    public float getPreDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraPreDistance.getValue() : preDistance.getValue();
    }

    public float getAttackDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraDistance.getValue() : distance.getValue();
    }

    @Override
    public void onDisable() {
        targetManager.releaseTarget();
        target = null;
        combatExecutor.combatManager().configurable(null);
        RotationManager.getInstance().resetRotation();
    }

    @Override
    public void onEnable() {
        targetManager.releaseTarget();
        target = null;
    }

    @Override
    public void onEvent() {
        EventListener eventUpdate = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            updateEventHandler();
        }));

        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            postRotMoveEventHandler();
        }));

        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            AuraUtil.onAttack(aimMode.getValue());
        }));

        addEvents(eventUpdate, rotationUpdateEvent, attackEvent);
    }

    private void postRotMoveEventHandler() {
        if (target == null) return;

        Vec3d attackVector = getTargetVector(target);
        Rotation rotation = RotationUtil.fromVec3d(attackVector.subtract(mc.player.getEyePos()));

        rotateToTarget(target, attackVector, rotation);
    }

    private void updateEventHandler() {
        target = updateTarget();

        if (target == null) {
            combatExecutor.combatManager().configurable(null);
            RotationManager.getInstance().resetRotation();
            return;
        }

        if (RotationUtil.getSpot(target).distanceTo(mc.player.getEyePos()) > getAttackDistance() + getPreDistance()) {
            targetManager.releaseTarget();
            target = null;
            combatExecutor.combatManager().configurable(null);
            RotationManager.getInstance().resetRotation();
            return;
        }

        attackTarget(target);
    }

    private LivingEntity updateTarget() {
        TargetManager.EntityFilter filter = new TargetManager.EntityFilter(targets.getList());
        targetManager.searchTargets(mc.world.getEntities(), getAttackDistance() + getPreDistance());
        targetManager.validateTarget(filter::isValid);
        return targetManager.getCurrentTarget();
    }

    private void attackTarget(LivingEntity target) {
        List<String> attackOptions = options.getList();
        if (usingElytraTarget()) {
            attackOptions = new java.util.ArrayList<>(attackOptions);
            attackOptions.remove("Raytrace");
        }

        combatExecutor.combatManager().configurable(
                new CombatExecutor.CombatConfigurable(
                        target,
                        RotationManager.getInstance().getRotation(),
                        distance.getValue(),
                        attackOptions
                )
        );

        // для проверки дистанции атаки всегда берём реальную точку на теле,
        // даже если ротация сейчас на отводке
        Vec3d attackCheckVec = getTargetVector(target);

        if (mc.player.getEyePos().distanceTo(
                RotationUtil.rayCastBox(target, attackCheckVec)
        ) > getAttackDistance()) return;

        combatExecutor.performAttack();
    }

    private void rotateToTarget(LivingEntity target, Vec3d targetVec, Rotation rotation) {
        boolean noHitRule = (!combatExecutor.combatManager().canAttack());

        if (usingElytraTarget() && ElytraTargetModule.getInstance().elytraRotationProcessor.customRotations.getValue()) return;

        if (noHitRule && aimMode.is("Snap") || noHitRule && aimMode.is("Funtime")) {
            if (!moveCorrection.getValue())
                return;
            else rotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        RotationStrategy configurable = new RotationStrategy(getRotationMode(),
                moveCorrection.getValue(), correctionMode.is("Свободная"));

        RotationManager.getInstance().addRotation(new Rotation.VecRotation(rotation, targetVec), target, configurable, TaskPriority.HIGH, this);
    }

    private RotationStrategy buildStrategy() {
        RotationStrategy strategy = new RotationStrategy(getRotationMode(),
                moveCorrection.getValue(), correctionMode.is("Свободная"))
                .clientLook(clientLook.getValue());

        // apply easing for smooth-style modes
        switch (aimMode.getValue()) {
            case "Smooth" -> strategy.easing(dev.ethereal.api.utils.animation.Easing.QUAD_OUT).easingStrength(0.35f);
            case "Funtime" -> strategy.easing(dev.ethereal.api.utils.animation.Easing.SINE_OUT).easingStrength(0.3f);
            case "Spooky Time" -> strategy.easing(dev.ethereal.api.utils.animation.Easing.CUBIC_OUT).easingStrength(0.25f);
        }

        return strategy;
    }

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "Smooth" -> new SmoothRotation();
            case "Snap" -> new InstantRotation();
            case "Funtime" -> new FunTimeRotation();
            case "Lony Grief" -> new LonyGriefRotation();
            case "Spooky Time" -> new SpookyTimeRotation();
            case "HvH" -> new HvHRotation();
            default -> new SmoothRotation();
        };
    }

    private Vec3d getTargetVector(LivingEntity target) {
        if (target == null) {
            return Vec3d.ZERO;
        }

        if (usingElytraTarget()) {
            return ElytraTargetModule.getInstance().elytraRotationProcessor.getPredictedPos(target);
        }

        return AuraUtil.getAimpoint(target, aimMode.getValue());
    }

    private boolean usingElytraTarget() {
        return target != null && ElytraTargetModule.getInstance().elytraRotationProcessor.using();
    }
}
