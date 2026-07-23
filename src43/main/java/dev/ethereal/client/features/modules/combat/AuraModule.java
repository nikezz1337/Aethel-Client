package dev.ethereal.client.features.modules.combat;
import dev.ethereal.api.utils.combat.ClickScheduler;
import dev.ethereal.api.utils.combat.TpsCalculator;
import dev.ethereal.api.utils.rotation.RaytracingUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.other.RotationUpdateEvent;
import dev.ethereal.api.event.events.player.world.AttackEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.player.move.VelocityEvent;
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
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import dev.ethereal.api.utils.rotation.manager.CombatRotation;
import dev.ethereal.client.features.modules.combat.elytratarget.ElytraTargetModule;
import dev.ethereal.api.utils.other.StopWatch;
import dev.ethereal.api.utils.player.InvUtil;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.player.PlayerUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import java.util.List;
import java.util.Random;
@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    @Getter private static final AuraModule instance = new AuraModule();
    private final TargetManager targetManager = new TargetManager();
    public final CombatExecutor combatExecutor = new CombatExecutor();
    @Getter private final ModeSetting aimMode = new ModeSetting("Ротация").value("Smooth").values(
            "Smooth", "Snap",
            "Funtime", "Spooky Time", "Spooky Time 2", "HolyWorld"
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
    public long lastLookUpTime = 0;
    public long nextLookUpDelay = java.util.concurrent.ThreadLocalRandom.current().nextLong(90000, 180000);
    public boolean isLookingUp = false;
    public long lookUpStartTime = 0;
    public int lookUpDuration = 0;
    public static int tick = 0;
    ClickScheduler click = new ClickScheduler();
    @Getter @Setter
    private CombatExecutor.CombatConfigurable configurable;


    public AuraModule() {
        addSettings(aimMode, distance, preDistance, targets, options, clientLook,
                elytraOverride, elytraDistance, elytraPreDistance,
                moveCorrection, correctionMode
        );

        VelocityEvent.getInstance().subscribe(new Listener<>(event -> {
            if (moveCorrection.getValue() && RotationComponent.getInstance().isRotating()) {
                Rotation rotation = RotationComponent.getInstance().currentRotation();
                if (rotation != null) {
                    event.setVelocity(Entity.movementInputToVelocity(event.getMovementInput(), event.getSpeed(), rotation.getYaw()));
                }
            }
        }));
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
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            AuraUtil.onAttack(aimMode.getValue());
        }));
        addEvents(eventUpdate, attackEvent);
    }
    private void postRotMoveEventHandler() {
        if (target == null) return;
        boolean canAttack = combatExecutor.combatManager().canAttack();
        boolean canAttack2 = click.isOneTickBeforeAttack();
        boolean check = false;
        switch (aimMode.getValue()) {
            case "Smooth" -> CombatRotation.rotationDef(target, canAttack);
            case "Snap" -> CombatRotation.rotSnap(target, canAttack, true);
            case "Funtime" -> CombatRotation.rotFT(target, canAttack, getAttackDistance(), check);
            case "Lony Grief" -> CombatRotation.slothOld(target, canAttack);
            case "HolyWorld" -> CombatRotation.rotHW(target, canAttack, true, false);
            case "Spooky Time" -> CombatRotation.rotST(target, canAttack2);
//            case "Spooky Time 2" -> CombatRotation.rotST2(target, canAttack2);
            case "HvH" -> CombatRotation.slothOld(target, canAttack);
        }
    }
    private void updateEventHandler() {
        target = updateTarget();
        if (target == null) {
            combatExecutor.combatManager().configurable(null);
            return;
        }
        if (RotationUtil.getSpot(target).distanceTo(mc.player.getEyePos())
                > getAttackDistance() + getPreDistance()) {
            targetManager.releaseTarget();
            target = null;
            combatExecutor.combatManager().configurable(null);
            return;
        }

        attackTarget(target);
        postRotMoveEventHandler();
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
                        RotationComponent.getInstance().currentRotation() != null ? RotationComponent.getInstance().currentRotation() : new Rotation(mc.player.getYaw(), mc.player.getPitch()),
                        getAttackDistance(),
                        attackOptions
                )
        );
        Vec3d attackCheckVec = getTargetVector(target);
        if (mc.player.getEyePos().distanceTo(
                RotationUtil.rayCastBox(target, attackCheckVec)
        ) > getAttackDistance()) return;
        combatExecutor.performAttack();
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
