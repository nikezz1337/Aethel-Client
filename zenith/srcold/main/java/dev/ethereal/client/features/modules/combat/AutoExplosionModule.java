package dev.ethereal.client.features.modules.combat;

import dev.ethereal.api.utils.rotation.rotations.HvHRotation;
import dev.ethereal.api.utils.rotation.rotations.SmoothRotation;
import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.world.BlockPlaceEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.rotation.RaytracingUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import dev.ethereal.api.utils.rotation.manager.RotationMode;
import dev.ethereal.api.utils.rotation.manager.RotationStrategy;
import dev.ethereal.api.utils.rotation.rotations.InstantRotation;
import dev.ethereal.api.utils.rotation.rotations.LonyGriefRotation;
import dev.ethereal.api.utils.task.TaskPriority;

import java.util.List;

@ModuleRegister(name = "Auto Explosion", category = Category.COMBAT)
public class AutoExplosionModule extends Module {
    @Getter private static final AutoExplosionModule instance = new AutoExplosionModule();

    private final MultiBooleanSetting options = new MultiBooleanSetting("Опции").value(
            new BooleanSetting("Safe").value(true),
            new BooleanSetting("Legit").value(false)
    );

    public AutoExplosionModule() {
        addSettings(options);
    }

    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil placeTimer = new TimerUtil();
    private final TimerUtil swapBackTimer = new TimerUtil();
    private final TimerUtil rotationTimer = new TimerUtil();

    private Entity crystalEntity = null;
    private BlockPos obsidianPos = null;

    private int prevSlot = -1;
    private int currentSlot = -1;
    private int bestSlot = -1;

    private boolean swapBack = false;

    private Runnable placeRunnable = null;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEvent() {
        EventListener blockPlaceEvent = BlockPlaceEvent.getInstance().subscribe(new Listener<>(event -> {
            handlePlaceEvent(event);
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            handleTickEvent();
        }));

        addEvents(tickEvent, blockPlaceEvent);
    }

    private void handlePlaceEvent(BlockPlaceEvent.BlockPlaceEventData event) {
        if (event.state().getBlock() == Blocks.OBSIDIAN || event.state().getBlock() == Blocks.BEDROCK) {
            obsidianPos = event.pos();
            boolean isOffhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;

            int slotInv = InventoryUtil.findItem(Items.END_CRYSTAL, false);
            int slotHb = InventoryUtil.findItem(Items.END_CRYSTAL, true);
            bestSlot = InventoryUtil.findEmptySlot();

            if (options.isEnabled("Legit") && mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                swapBackTimer.reset();
            }

            if (isOffhand) {
                if (obsidianPos != null) {
                    placeRunnable = () -> placeCrystal(bestSlot, obsidianPos);
                    placeTimer.reset();
                }
            } else if (slotHb == -1 && slotInv != -1 && bestSlot != -1) {
                placeRunnable = () -> {
                    if (SlownessManager.isEnabled()) {
                        SlownessManager.applySlowness(10, () -> {
                            funnyBabah(slotInv);
                        });
                    } else {
                        funnyBabah(slotInv);
                    }
                };
                placeTimer.reset();
            } else if (slotHb != -1) {
                placeRunnable = () -> {
                    if (obsidianPos == null) {
                        return;
                    }

                    prevSlot = mc.player.getInventory().selectedSlot;
                    placeCrystal(slotHb, obsidianPos);

                    if (options.isEnabled("Legit")) {
                        swapBackTimer.reset();
                        swapBack = true;
                        currentSlot = mc.player.getInventory().selectedSlot;
                    } else {
                        mc.player.getInventory().selectedSlot = prevSlot;
                    }
                };
                placeTimer.reset();
            }
        }
    }

    private void handleTickEvent() {
        if (crystalEntity != null && !crystalEntity.isAlive()) {
            reset();
        }

        if (placeRunnable != null && placeTimer.finished(50)) {
            placeRunnable.run();
            placeTimer.reset();
            placeRunnable = null;
        }

        if (obsidianPos != null && attackTimer.finished(50)) {
            for (EndCrystalEntity crystal : findCrystals(obsidianPos)) {
                if (isValid(crystal)) {
                    attackCrystal(crystal);
                }
            }
        }

        if (options.isEnabled("Legit") && swapBack) {
            int playerCurrentSlot = mc.player.getInventory().selectedSlot;

            if (playerCurrentSlot != currentSlot && playerCurrentSlot != prevSlot) {
                swapBack = false;
                return;
            }

            if (swapBackTimer.finished(50)) {
                mc.player.getInventory().selectedSlot = prevSlot;
                swapBack = false;
            }
        }
    }

    private void attackCrystal(Entity entity) {
        if (isValid(entity) &&
                mc.player.getAttackCooldownProgress(1f) >= 1f) {

            Rotation targetRotation = rotate(entity);
            
            // Используем целевую rotation для raytrace, а не текущую rotation игрока
            EntityHitResult hitResult = RaytracingUtil.raytraceEntity(3, targetRotation, false);

            if (hitResult != null && hitResult.getEntity() == entity) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
                attackTimer.reset();

                crystalEntity = entity;
            }
        }

        if (!entity.isAlive()) {
            crystalEntity = null;
            obsidianPos = null;
        }
    }

    private Rotation rotate(Entity entity) {
        Vec3d targetPos = RotationUtil.getSpot(entity);
        Rotation rotations = RotationUtil.rotationAt(targetPos);

        AuraModule aura = AuraModule.getInstance();
        RotationStrategy configurable = new RotationStrategy(new HvHRotation(), aura.moveCorrection.getValue(), aura.correctionMode.is("Свободная")).ticksUntilReset(3);

        RotationManager.getInstance().addRotation(rotations, configurable, TaskPriority.REQUIRED, this);

        return rotations;
    }


    private void placeCrystal(int slot, BlockPos pos) {
        boolean isOffhand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
        Vec3d center = Vec3d.ofCenter(pos);
        BlockHitResult hitResult = new BlockHitResult(center, Direction.UP, pos, false);

        if (isOffhand) {
            if (mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hitResult).isAccepted()) {
                mc.player.swingHand(Hand.OFF_HAND);
            }
        } else {
            mc.player.getInventory().selectedSlot = slot;
            if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult).isAccepted() &&
                    mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private boolean isValid(Entity entity) {
        if (entity == null || obsidianPos == null || !entity.isAlive()) {
            return false;
        }

        if (options.isEnabled("Safe")) {
            if (mc.player.getY() > obsidianPos.getY()) {
                return false;
            }
        }

        return mc.player.getEyePos().distanceTo(RotationUtil.getSpot(entity)) < 3;
    }

    private List<EndCrystalEntity> findCrystals(BlockPos pos) {
        return mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                new Box(pos).expand(1.0, 2.0, 1.0),
                endCrystalEntity -> endCrystalEntity != null && endCrystalEntity.isAlive()
        );
    }

    private void funnyBabah(int slot) {
        InventoryUtil.swapSlots(slot, bestSlot);
        if (obsidianPos != null) {
            prevSlot = mc.player.getInventory().selectedSlot;
            placeCrystal(bestSlot, obsidianPos);

            if (options.isEnabled("Legit")) {
                swapBackTimer.reset();
                swapBack = true;
                currentSlot = mc.player.getInventory().selectedSlot;
            } else {
                mc.player.getInventory().selectedSlot = prevSlot;
            }
        }
        InventoryUtil.swapSlots(bestSlot, slot);
    }

    private void reset() {
        crystalEntity = null;
        obsidianPos = null;
        prevSlot = -1;
        bestSlot = -1;
        swapBack = false;
        currentSlot = -1;

        placeTimer.reset();
        attackTimer.reset();
        swapBackTimer.reset();
        rotationTimer.reset();

        placeRunnable = null;
    }
}
