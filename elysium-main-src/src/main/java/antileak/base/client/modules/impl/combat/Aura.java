package antileak.base.client.modules.impl.combat;

import com.adl.nativeprotect.Native;
import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import antileak.base.elysium;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventAttackEntity;
import antileak.base.api.events.implement.EventGameUpdate;
import antileak.base.api.events.implement.EventMoveInput;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.events.implement.EventUpdatePost;
import antileak.base.api.storages.implement.FreeLookStorage;
import antileak.base.api.storages.implement.NeuroAuraStorage;
import antileak.base.api.storages.implement.RotationStorage;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.combat.IdealHitUtils;
import antileak.base.api.utils.combat.RayTraceUtil;
import antileak.base.api.utils.math.MathUtils;
import antileak.base.api.utils.math.TimerUtils;
import antileak.base.api.utils.player.InventoryUtils;
import antileak.base.api.utils.rotate.MultipointUtils;
import antileak.base.api.utils.rotate.Rotation;
import antileak.base.api.utils.rotate.RotationUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.impl.combat.components.RotationsSystem;
import antileak.base.client.modules.impl.combat.components.interpolation.BestPoint;
import antileak.base.client.modules.impl.combat.components.gcd.GCDUtil;
import antileak.base.client.modules.impl.combat.components.rotations.SlothRotation;
import antileak.base.client.modules.impl.combat.components.rotations.TestRotation;
import antileak.base.client.modules.impl.combat.components.rotations.WellMineRotation;
import antileak.base.client.modules.impl.combat.components.rotations.WhiteRiseRotation;
import antileak.base.client.modules.impl.movement.AirStuck;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.FloatSetting;
import antileak.base.client.modules.settings.implement.ListSetting;
import antileak.base.client.modules.settings.implement.ModeSetting;
import antileak.base.mixin.ILivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class Aura extends Module {

    public static Aura INSTANCE = new Aura();

    public final ModeSetting rotationType = new ModeSetting("Ротация", "Smooth",
            "Smooth", "HolyWorld", "Data", "Sloth", "NoRotate");

    private final ListSetting targets = new ListSetting("Таргеты",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Невидимки", true),
            new BooleanSetting("Мирные", false),
            new BooleanSetting("Мобы", true)
    );

    public final FloatSetting range = new FloatSetting("Дистанция атаки", 3f, 0f, 6f, 0.05f);
    private final FloatSetting aimRange = new FloatSetting("Дистанция наводки", 3f, 0f, 6f, 0.05f);
    private final FloatSetting elytraAimRange = new FloatSetting("Дистанция на элитрах", 50f, 10f, 100f, 0.05f);
    public final BooleanSetting smartCrit = new BooleanSetting("Умные криты", false);
    public final BooleanSetting sprintReset = new BooleanSetting("Сброс спринта", true);
    private final BooleanSetting throughWalls = new BooleanSetting("Бить через стены", true);
    private final BooleanSetting raycast = new BooleanSetting("Проверка на наведение", false);
    private final BooleanSetting unpressShield = new BooleanSetting("Отжимать щит", false);
    private final BooleanSetting breakShield = new BooleanSetting("Ломать щит", true);
    private final BooleanSetting attackOnEating = new BooleanSetting("Не бить когда ешь", true);
    public static BooleanSetting clientLook = new BooleanSetting("Наводка от первого лица", false);
    private final ModeSetting priority = new ModeSetting("Приоритет", "Дистанция", "Дистанция", "Здоровье", "Угол", "Никакой");

    @Getter
    private LivingEntity target;
    @Getter
    private Vec2f currentRotations = new Vec2f(0f, 0f);
    @Getter
    private Vec2f targetRotations = new Vec2f(0f, 0f);
    @Getter
    private final NeuroAuraStorage dataSystem = new NeuroAuraStorage();
    @Getter
    private final TimerUtils attackTimer = new TimerUtils();
    private final BooleanSetting rwWallBypass = new BooleanSetting("Обход рв стен", false);
    private final BooleanSetting syncTps = new BooleanSetting("Синхронизировать с ТПСом", false);
    private final WellMineRotation wellMineRotation = new WellMineRotation();
    private final TestRotation testRotation = new TestRotation();
    private final SlothRotation slothRotation = new SlothRotation();
    private final WhiteRiseRotation whiteRiseRotation = new WhiteRiseRotation(this);
    private final TimerUtils backTimer = new TimerUtils();

    private long cps = 0;
    @Getter
    private boolean needSprintReset = false;
    private boolean sprintResetDone = false;
    private int sprintResetTicks = 0;
    private int ticksToAttack = 0;
    private int bypassAttackAge = -1;
    private boolean bypassAttackQueued = false;
    private LivingEntity lastDataTarget = null;
    private LivingEntity holyWorldTarget = null;
    private float holyWorldSpeedAcceleration = 0f;
    private boolean holyWorldBack = false;

    private float lastYaw = 0f;
    private float lastPitch = 0f;

    public static float adjYaw;
    public static float adjPitch;
    public static float otvodkaYaw;
    public static float otvodkaPitch;

    public boolean isRotated;

    public Aura() {
        super("AttackAura", "Автоматически наводиться и бьёт таргета", ModuleCategory.COMBAT);
        addSettings(rotationType, targets, range, aimRange, elytraAimRange, smartCrit, sprintReset, syncTps,
                attackOnEating, throughWalls, rwWallBypass, raycast, unpressShield, breakShield, clientLook, priority);
    }
    @Native
    @EventLink
    public void onPlayerTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        lastYaw++;
        updateTarget();

        if (dataSystem.isRecording()) {
            LivingEntity recordTarget = findTargetForRecording();
            dataSystem.recordTick(recordTarget, mc.player.getYaw(), mc.player.getPitch());
        }
    }
    @Native
    @EventLink
    public void onAttackEntity(EventAttackEntity event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getPlayer() != mc.player) return;
        if (!(event.getTarget() instanceof LivingEntity living)) return;
        if (!isValidTarget(living)) return;

        target = living;
    }

    @EventLink
    public void onMoveInput(EventMoveInput event) {
        if (needSprintReset) {
            event.setForward(0);
            event.setStrafe(0);
            needSprintReset = false;
            sprintResetDone = true;
            sprintResetTicks = 0;
        }
    }

    @EventLink
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null || mc.world == null || target == null) return;
        rotate();
    }
    @Native
    @EventLink
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (ticksToAttack > 0) {
            ticksToAttack--;
        }

        if (sprintResetDone) {
            sprintResetTicks++;
        }

        boolean packetCrits = ModuleClass.packetCriticals.isEnable()
                && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);

        if (!packetCrits) {
            processAttack();
        }

        if (dataSystem.isShowStats() && mc.player.age % 40 == 0 && (dataSystem.isRecording() || dataSystem.isUsingNeuro())) {
            mc.player.sendMessage(net.minecraft.text.Text.literal(dataSystem.getStatusString()), true);
        }
    }

    @EventLink
    public void onPost(EventUpdatePost e) {
        if (mc.player == null || mc.world == null) return;
        boolean packetCrits = ModuleClass.INSTANCE.packetCriticals.isEnable()
                && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);

        if (packetCrits && mc.player.fallDistance > 0 && mc.player.fallDistance < 1) {
            processAttack();
        }
    }
    @Native
    private LivingEntity findTargetForRecording() {
        LivingEntity bestTarget = null;
        double bestDistance = 100.0;
        Vec3d eyePos = mc.player.getEyePos();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive() || living.getHealth() <= 0) continue;
            if (living instanceof ArmorStandEntity) continue;

            double distance = eyePos.squaredDistanceTo(living.getBoundingBox().getCenter());
            if (distance > bestDistance) continue;

            bestDistance = distance;
            bestTarget = living;
        }

        return bestTarget;
    }
    @Native
    private void processAttack() {
        updateTarget();

        if (target != null) {
            if (shouldAttack() && cps <= System.currentTimeMillis()) {
                if (attackOnEating.isState() && mc.player.isUsingItem()) return;

                if (sprintReset.isState() && mc.player.isSprinting() && !sprintResetDone) {
                    needSprintReset = true;
                    return;
                }

                if (sprintReset.isState() && sprintResetDone && sprintResetTicks < 1) {
                    return;
                }

                if (isBypassRotationActive() && !prepareBypassAttack()) {
                    return;
                }
                attack();
                resetBypassAttack();
                sprintResetDone = false;
                sprintResetTicks = 0;
            }
        } else {
            cps = System.currentTimeMillis();
            backTimer.reset();
            adjPitch = 0;
            adjYaw = 0;
            wellMineRotation.reset();
            testRotation.reset();
            slothRotation.reset();
            whiteRiseRotation.reset();
            dataSystem.resetState();
            lastDataTarget = null;
            resetHolyWorldRotation();
            sprintResetDone = false;
            sprintResetTicks = 0;
            ticksToAttack = 0;
            resetBypassAttack();
        }
    }

    public void Rotate() {
        rotate();
    }
    @Native
    private void rotate() {

        if (target == null) return;

        if (rotationType.is("Data") && target != lastDataTarget) {
            dataSystem.resetState();
            lastDataTarget = target;
        }

        if (isBypassRotationActive()) {
            updateBypassRotation(target);
            return;
        }

        RotationsSystem system;

        if (rotationType.is("HolyWorld")) {
            updateHolyWorldRotation(target);
            return;
        } else if (rotationType.is("Smooth")) {
            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    Vec3d aimPoint = getPredictedRotationPoint(target, target.getBoundingBox().getCenter());
                    Vec2f rot = RotationUtils.getRotations(aimPoint);
                    targetRotations = rot;
                    currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                    RotationStorage.update(new Rotation(rot.x, rot.y), 360, 360, 360, 360, 1, 1, clientLook.isState());
                }
            };
        } else if (rotationType.is("WellMine")) {
            system = wellMineRotation;
        } else if (rotationType.is("Test")) {
            system = testRotation;
        } else if (rotationType.is("Sloth")) {
            system = whiteRiseRotation;
        } else if (rotationType.is("NoRotate")) {
            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    RotationStorage.update(new Rotation(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch()), MathUtils.random(100, 170), MathUtils.random(100, 170), MathUtils.random(100, 170), MathUtils.random(100, 170), 1, 6, false);
                }
            };
        } else if (rotationType.is("Data")) {
            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    boolean focusRotation = shouldFocusDataRotation();
                    Rotation rotation;

                    rotation = dataSystem.getNeuroRotation(
                            target,
                            mc.player.getYaw(),
                            mc.player.getPitch(),
                            focusRotation
                    );

                    if (rotation == null) {
                        Vec3d point = MultipointUtils.getClosestPoint(target);
                        Vec2f rot = RotationUtils.getRotations(getPredictedPoint(target, point != null ? point : target.getBoundingBox().getCenter()));
                        rotation = new Rotation(rot.x, rot.y);
                    }

                    targetRotations = new Vec2f(rotation.getYaw(), rotation.getPitch());
                    currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());

                    RotationStorage.update(
                            rotation,
                            focusRotation ? 24.0f : 11.5f,
                            focusRotation ? 18.0f : 9.0f,
                            focusRotation ? 18.0f : 9.0f,
                            focusRotation ? 14.0f : 7.0f,
                            1,
                            1, clientLook.isState()
                    );
                }
            };
        } else {

            Vec2f targetRot;
            targetRot = RotationUtils.getRotations(getPredictedRotationPoint(target, target.getLeashPos(1)));

            system = new RotationsSystem() {
                @Override
                public void updateRotations(LivingEntity target) {
                    currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                    RotationStorage.update(new Rotation(targetRot.x, targetRot.y), 360, 360, 360, 360, 1, 1, clientLook.isState());
                }
            };
        }

        system.updateRotations(target);
    }
    @Native
    private void updateBypassRotation(LivingEntity target) {
        Vec3d point = MultipointUtils.getClosestPoint(target);
        if (point == null) {
            point = target.getBoundingBox().getCenter();
        }

        Vec3d predicted = getPredictedRotationPoint(target, point);
        Vec2f targetRot = RotationUtils.getRotations(predicted);

        targetRotations = targetRot;
        currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());

        boolean isBypassAttackTick = mc.player.age <= bypassAttackAge;

        float finalYaw = targetRot.x;
        float finalPitch = targetRot.y;
        if (!isBypassAttackTick) {
            finalYaw = FreeLookStorage.getFreeYaw();
            finalPitch = FreeLookStorage.getFreePitch();
        }

        RotationStorage.update(new Rotation(finalYaw, finalPitch), 360f, 360f, 360f, 360f, 0, 6, clientLook.isState());
    }

    private boolean isBypassRotationActive() {
        return isUsingRwWallBypass();
    }

    private boolean prepareBypassAttack() {
        if (!bypassAttackQueued) {
            bypassAttackQueued = true;
            bypassAttackAge = mc.player.age + 1;
            return false;
        }

        if (mc.player.age > bypassAttackAge) {
            resetBypassAttack();
            return false;
        }

        return isBypassAimReadyForAttack();
    }
    @Native
    private boolean isBypassAimReadyForAttack() {
        if (target == null || mc.player == null) {
            return false;
        }

        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetRotations.x - mc.player.getYaw()));
        float pitchDiff = Math.abs(targetRotations.y - mc.player.getPitch());
        boolean onTarget = isUsingRwWallBypass() || isCurrentAimOnTarget();

        return yawDiff <= 3.0f && pitchDiff <= 2.5f && onTarget;
    }

    private boolean isUsingRwWallBypass() {
        return rwWallBypass.isState() && target != null && isTargetBehindWall(target);
    }

    private EntityHitResult getAttackRaycastResult() {
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        float reach = getEffectiveRange() * 2.0f;
        Vec3d reachVec = eyePos.add(lookVec.multiply(reach));

        return ProjectileUtil.raycast(
                mc.player,
                eyePos,
                reachVec,
                mc.player.getBoundingBox().expand(reach),
                ex -> ex != mc.player && ex.isAlive(),
                reach * reach
        );
    }

    private boolean isTargetBehindWall(LivingEntity entity) {
        return entity != null && !mc.player.canSee(entity);
    }
    @Native
    private Vec3d getPredictedRotationPoint(LivingEntity target, Vec3d point) {
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        if (mc.player != null
                && target != null
                && elytraTarget != null
                && elytraTarget.isPredictionActive()) {
            return elytraTarget.getPredictedPoint(target, point);
        }
        return point;
    }
    @Native
    private boolean isCurrentAimOnTarget() {
        if (target == null || mc.player == null) {
            return false;
        }

        if (mc.player.isGliding() && target.isGliding()) {
            return RayTraceUtil.rayTraceEntity(mc.player.getYaw(), mc.player.getPitch(), getMaxAimRange(), target, false);
        }

        EntityHitResult result = getAttackRaycastResult();
        return result != null && result.getEntity() == target;
    }
    @Native
    private LivingEntity findTarget() {
        List<LivingEntity> entities = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living)) continue;
            entities.add(living);
        }

        if (entities.isEmpty() || !isEnable()) return null;

        switch (priority.getCurrent()) {
            case "Дистанция" ->
                    entities.sort(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().squaredDistanceTo(mc.player.getEyePos())));
            case "Здоровье" -> entities.sort(Comparator.comparingDouble(LivingEntity::getHealth));
            case "Угол" -> entities.sort(Comparator.comparingDouble(entity -> {
                Vec2f vec = RotationUtils.getRotations(entity.getBoundingBox().getCenter());
                double dy = Math.abs(wrapDegrees(vec.x - mc.player.getYaw()));
                double dp = Math.abs(wrapDegrees(vec.y - mc.player.getPitch()));
                return dy + dp;
            }));
            case "Никакой" -> {
            }
        }

        return entities.isEmpty() ? null : entities.get(0);
    }
    @Native
    private void updateTarget() {
        if (!isEnable()) {
            target = null;
            return;
        }

        if (target != null && isValidTarget(target)) {
            return;
        }

        target = findTarget();
    }
    @Native
    private boolean shouldFocusDataRotation() {
        float cooldown = mc.player.getAttackCooldownProgress(1.5f);
        float focusThreshold = Math.max(0.82f, IdealHitUtils.getAICooldown() - 0.08f);
        boolean readyByCooldown = cooldown >= focusThreshold;
        boolean fallingForCrit = !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0.0
                && mc.player.fallDistance > 0.0f;

        return readyByCooldown || fallingForCrit;
    }
    @Native
    private void attack() {
        if (unpressShield.isState() && mc.player.isBlocking()) mc.interactionManager.stopUsingItem(mc.player);
        tryBreakRwWallBlockPacket();

        boolean attacked = false;
        if (target instanceof PlayerEntity player && player.isBlocking() && breakShield.isState()) {
            attacked = shieldBreak(player);
        }

        if (!attacked) {
            mc.interactionManager.attackEntity(mc.player, target);
        }

        mc.player.swingHand(Hand.MAIN_HAND);

        long cooldown = 467L;

        if (syncTps.isState()) {
            cooldown = (long) (getTpsAdjustedCooldown(cooldown) * 1.1f);
        }

        cps = System.currentTimeMillis() + cooldown;
        ticksToAttack = 10;
        attackTimer.reset();
    }

    private float getSyncTpsValue() {
        if (elysium.INSTANCE == null || elysium.INSTANCE.tpsCalc == null) {
            return 20.0f;
        }
        float tps = elysium.INSTANCE.tpsCalc.getTPS();
        return MathHelper.clamp(tps, 0.1f, 20.0f);
    }
    @Native
    private long getTpsAdjustedCooldown(long baseCooldown) {
        if (!syncTps.isState()) {
            return baseCooldown;
        }
        float tps = getSyncTpsValue();
        if (tps >= 20.0f) {
            return baseCooldown;
        }
        float multiplier = 20.0f / tps;
        float additionalFactor = 1.0f + (20.0f - tps) * 0.05f;
        long adjusted = (long) (baseCooldown * multiplier * additionalFactor);
        return Math.min(adjusted, 3000);
    }
    @Native
    private void tryBreakRwWallBlockPacket() {
        if (!rwWallBypass.isState() || target == null || mc.player == null || mc.world == null) return;
        if (mc.player.canSee(target)) return;
        if (mc.player.networkHandler == null) return;

        Vec3d startVec = mc.player.getEyePos();
        Vec3d targetPos = getPredictedRotationPoint(target, target.getEyePos());
        Vec3d direction = targetPos.subtract(startVec);
        double distance = direction.length();

        if (distance < 1.0E-3D) return;

        Vec3d normalizedDir = direction.normalize();
        for (double i = 0.0D; i < distance; i += 0.5D) {
            Vec3d point = startVec.add(normalizedDir.multiply(i));
            BlockPos pos = BlockPos.ofFloored(point);

            if (mc.world.getBlockState(pos).isAir()) {
                continue;
            }

            if (mc.world.getBlockState(pos).getHardness(mc.world, pos) < 0.0F) {
                continue;
            }

            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
    }

    private boolean shieldBreak(PlayerEntity entity) {
        if (mc.player == null || mc.interactionManager == null || entity == null) {
            return false;
        }

        int axeHotbarSlot = findAxeHotbarSlot();
        if (axeHotbarSlot != -1) {
            attackWithHotbarSlot(entity, axeHotbarSlot);
            return true;
        }

        int axeInventorySlot = findAxeInventorySlot();
        if (axeInventorySlot == -1) {
            return false;
        }

        int selectedSlot = mc.player.getInventory().selectedSlot;
        int containerSlot = InventoryUtils.toContainerSlot(axeInventorySlot);

        mc.interactionManager.clickSlot(0, containerSlot, selectedSlot, SlotActionType.SWAP, mc.player);
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));

        try {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
            mc.interactionManager.attackEntity(mc.player, entity);
            return true;
        } finally {
            mc.interactionManager.clickSlot(0, containerSlot, selectedSlot, SlotActionType.SWAP, mc.player);
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
        }
    }

    private void attackWithHotbarSlot(PlayerEntity entity, int slot) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        if (slot != previousSlot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }

        try {
            mc.interactionManager.attackEntity(mc.player, entity);
        } finally {
            if (slot != previousSlot) {
                mc.player.getInventory().selectedSlot = previousSlot;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            }
        }
    }

    private int findAxeHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private int findAxeInventorySlot() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean isWeapon() {
        Item item = mc.player.getMainHandStack().getItem();
        return item != Items.AIR && (item instanceof SwordItem
                || item instanceof PickaxeItem
                || item instanceof AxeItem
                || item instanceof HoeItem
                || item instanceof ShovelItem
                || item instanceof MaceItem
                || item == Items.MACE);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0) return false;
        if (entity instanceof ArmorStandEntity) return false;

        if (AntiBot.checkBot(entity)) return false;

        if (entity instanceof PlayerEntity player) {
            if (!targets.is("Игроки")) return false;
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY) && !targets.is("Невидимки")) return false;
            if (elysium.INSTANCE.friendStorage.isFriend(entity.getName().getString())) return false;
        } else if (entity instanceof HostileEntity) {
            if (!targets.is("Мобы")) return false;
        } else {
            if (!targets.is("Мирные")) return false;
        }

        Vec3d nearestPoint = BestPoint.getNearestPoint(entity);
        if (nearestPoint == null) nearestPoint = MultipointUtils.getClosestPoint(entity);
        if (mc.player.getEyePos().distanceTo(nearestPoint) > getMaxAimRange()) return false;
        if (!throughWalls.isState() && !rwWallBypass.isState() && !mc.player.canSee(entity)) return false;

        return true;
    }

    private boolean shouldAttack() {
        if (mc.player.getAttackCooldownProgress(1.5f) < IdealHitUtils.getAICooldown()) {
            return false;
        }
        EntityHitResult result = getAttackRaycastResult();
        boolean aimOnTarget = isCurrentAimOnTarget();
        if (raycast.isState() && !isUsingRwWallBypass() && !aimOnTarget) {
            return false;
        }
        if (rotationType.is("Data") && !isUsingRwWallBypass() && !isDataAimReady(result, aimOnTarget)) {
            return false;
        }
        if (mc.player.isGliding() && target.isGliding()) {
            ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
            double currentDistance;
            if (elytraTarget != null && elytraTarget.isPredictionActive()) {
                if (elytraTarget.hasChasePosition()) {
                    currentDistance = elytraTarget.getPredictedDistance();
                } else {
                    Vec3d aimPoint = elytraTarget.getAimPoint(target);
                    currentDistance = aimPoint != null
                            ? mc.player.getEyePos().distanceTo(aimPoint)
                            : mc.player.getEyePos().distanceTo(target.getEyePos());
                }
            } else {
                currentDistance = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
            }
            if (currentDistance > getEffectiveRange()) return false;
            return true;
        } else {
            double distanceCheck = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
            Vec3d checkPoint = distanceCheck > 3 ? BestPoint.getNearestPoint(target) : target.getBoundingBox().getCenter();
            if (checkPoint == null) checkPoint = MultipointUtils.getClosestPoint(target);
            if (mc.player.getEyePos().distanceTo(checkPoint) > getEffectiveRange()) return false;
            return IdealHitUtils.canCritical(target);
        }
    }

    public int getWhiteRiseTicksToAttack() {
        return ticksToAttack;
    }

    private boolean isDataAimReady(EntityHitResult result, boolean aimOnTarget) {
        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetRotations.x - mc.player.getYaw()));
        float pitchDiff = Math.abs(targetRotations.y - mc.player.getPitch());
        boolean closeToAim = yawDiff <= 1.15f && pitchDiff <= 0.9f;
        boolean onTarget = (mc.player.isGliding() && target != null && target.isGliding())
                ? aimOnTarget
                : result != null && result.getEntity() == target;

        return closeToAim && onTarget;
    }

    public boolean isAboveWater() {
        BlockPos pos = BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0));
        return !mc.player.isSubmergedInWater() && mc.world.getBlockState(pos).isOf(Blocks.WATER);
    }

    public float getAttackCooldown() {
        return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks()) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
    }

    public float getAttackCooldownProgressPerTick() {
        return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED) * 20);
    }

    private float getMaxAimRange() {
        return mc.player.isGliding()
                ? elytraAimRange.getValue().floatValue()
                : getEffectiveRange() + aimRange.getValue().floatValue();
    }

    private float getEffectiveRange() {
        float base = range.getValue().floatValue();
        if (AirStuck.INSTANCE.isEnable() && AirStuck.INSTANCE.extraRangeEnabled.isState()) {
            base += AirStuck.INSTANCE.extraRange.getValue().floatValue();
        }
        return base;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (target != null) backTimer.reset();
        target = null;
        wellMineRotation.reset();
        testRotation.reset();
        slothRotation.reset();
        whiteRiseRotation.reset();
        dataSystem.resetState();
        lastDataTarget = null;
        resetHolyWorldRotation();
        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
        ticksToAttack = 0;
        resetBypassAttack();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        wellMineRotation.reset();
        testRotation.reset();
        slothRotation.reset();
        whiteRiseRotation.reset();
        dataSystem.resetState();
        lastDataTarget = null;
        resetHolyWorldRotation();
        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
        ticksToAttack = 0;
        resetBypassAttack();
        if (mc.player != null) {
            currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    private void resetBypassAttack() {
        bypassAttackAge = -1;
        bypassAttackQueued = false;
    }

    private void resetHolyWorldRotation() {
        holyWorldTarget = null;
        holyWorldSpeedAcceleration = 0.0F;
        holyWorldBack = false;

        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        } else {
            lastYaw = 0.0F;
            lastPitch = 0.0F;
        }
    }

    private void updateHolyWorldRotation(LivingEntity target) {
        if (mc.player == null || target == null) {
            return;
        }

        if (holyWorldTarget != target) {
            holyWorldTarget = target;
            holyWorldSpeedAcceleration = 0.0F;
            holyWorldBack = false;
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }

        long currentTime = System.currentTimeMillis();
        double randomFactor = Math.random();
        double timeSineFactor = Math.sin((double) currentTime / 1000.0) * Math.cos((double) currentTime / 450.0);
        Vec3d playerVelocity = mc.player.getVelocity();
        boolean playerFlying = mc.player.isGliding();
        boolean isMoving = mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F;
        Vec3d targetPoint = target.getBoundingBox().getCenter();

        if (isMoving) {
            double velocityInfluence = 0.045 + randomFactor * 0.055;
            targetPoint = targetPoint.add(
                    playerVelocity.x * (0.3 + timeSineFactor * 0.2) + (randomFactor - 0.5) * velocityInfluence,
                    (randomFactor - 0.7) * 0.04,
                    playerVelocity.z * (0.35 + timeSineFactor * 0.2) + (randomFactor - 0.6) * velocityInfluence
            );
        }

        if (target.isGliding()) {
            Vec3d targetVelocity = target.getVelocity();
            double predictValue = 0.5 + (timeSineFactor * 0.05);
            targetPoint = targetPoint.add(targetVelocity.multiply(predictValue));
        }

        Vec2f calculated = RotationUtils.getRotations(targetPoint);
        float targetYaw = calculated.x;
        float targetPitch = calculated.y;
        float yawDifference = Math.abs(MathHelper.wrapDegrees(targetYaw - this.lastYaw));
        boolean isReadyToAttack = mc.player.getAttackCooldownProgress(1.0F) > 0.93F;

        if (!this.holyWorldBack) {
            float accelerationRate = 0.0065F + (float) (randomFactor * 0.0075);
            if (yawDifference > 30.0F) {
                accelerationRate += 0.015F;
            }

            if (yawDifference > 10.0F && isReadyToAttack) {
                accelerationRate += 0.022F;
            }

            if (isMoving) {
                accelerationRate *= 0.95F + (float) randomFactor * 0.18F;
            }

            if (isReadyToAttack) {
                accelerationRate += 0.025F * (float) randomFactor;
            }

            this.holyWorldSpeedAcceleration += accelerationRate * (0.96F + (float) timeSineFactor * 0.14F);
            float maxAcceleration = isMoving ? 0.26F + (float) randomFactor * 0.05F : 0.29F + (float) randomFactor * 0.04F;
            if (this.holyWorldSpeedAcceleration >= maxAcceleration) {
                this.holyWorldBack = true;
            }
        } else {
            this.holyWorldSpeedAcceleration -= (isReadyToAttack ? 0.028F : 0.014F) * (1.0F + (float) randomFactor * 0.5F);
            if (this.holyWorldSpeedAcceleration <= -0.042F) {
                this.holyWorldBack = false;
            }
        }

        float maxSpeed = playerFlying ? 0.62F : (isMoving ? 0.45F : 0.35F);
        float currentSpeed = MathHelper.clamp(this.holyWorldSpeedAcceleration, 0.032F, maxSpeed);
        if (isReadyToAttack && randomFactor > 0.75) {
            currentSpeed = Math.min(currentSpeed + (float) (randomFactor * 0.08), maxSpeed + 0.06F);
        }

        float yawChange = MathHelper.wrapDegrees(targetYaw - this.lastYaw);
        float pitchChange = targetPitch - this.lastPitch;
        float maxYawChange = (playerFlying ? 85.0F : (isReadyToAttack ? 48.0F : 32.0F)) + (float) (randomFactor * 12.0);
        float maxPitchChange = (playerFlying ? 45.0F : (isReadyToAttack ? 28.0F : 20.0F)) + (float) (randomFactor * 10.0);

        if (isMoving && !isReadyToAttack) {
            maxYawChange *= 0.85F;
            maxPitchChange *= 0.85F;
        }

        yawChange = MathHelper.clamp(yawChange, -maxYawChange, maxYawChange);
        pitchChange = MathHelper.clamp(pitchChange, -maxPitchChange, maxPitchChange);
        float newYaw = this.lastYaw + yawChange * currentSpeed;
        float newPitch = MathHelper.clamp(
                this.lastPitch + pitchChange * currentSpeed * (isReadyToAttack ? 0.92F : 0.82F) * (float) (0.88 + randomFactor * 0.28) * (0.7F + (float) randomFactor * 0.15F),
                -89.2F,
                89.2F
        );

        float gcdValue = GCDUtil.getGCDValue();
        if (gcdValue > 0.0F) {
            newYaw = this.lastYaw + (float) Math.round((newYaw - this.lastYaw) / gcdValue) * gcdValue;
            newPitch = this.lastPitch + (float) Math.round((newPitch - this.lastPitch) / gcdValue) * gcdValue;
        }

        Rotation finalRotation = new Rotation(newYaw, newPitch);
        float rotationSpeed = playerFlying && target.isGliding() ? 360.0F : (isMoving ? 64.0F + (float) randomFactor * 28.0F : 75.0F + (float) randomFactor * 35.0F);
        targetRotations = new Vec2f(finalRotation.getYaw(), finalRotation.getPitch());
        currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
        RotationStorage.update(finalRotation, rotationSpeed, rotationSpeed, rotationSpeed, rotationSpeed, 0, 1, clientLook.isState());
        this.lastYaw = finalRotation.getYaw();
        this.lastPitch = finalRotation.getPitch();
    }
}
