package dev.ethereal.client.features.modules.movement.spider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.ButtonBlock;
import net.minecraft.client.util.InputUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.orbit.EventPriority;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.event.events.player.move.JumpEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;

import java.util.Random;
import java.util.function.Predicate;

@ModuleRegister(name = "Spider", category = Category.MOVEMENT)
public class SpiderModule extends Module {
    @Getter private static final SpiderModule instance = new SpiderModule();

    private static final double FT_COLLISION_CLIMB_MOTION = 0.044927534277258596D;
    private static final double FT_COLLISION_VERTICAL_BOOST = 0.45D;
    private static final long FT_COLLISION_DELAY_MS = 30L;
    private static final long FT_COLLISION_MICRO_JUMP_DELAY_MS = 30L;
    private static final long FT_COLLISION_JUMP_RELEASE_BUFFER_MS = 100L;
    private static final long LEVER_PLACE_DELAY_MS = 110L;
    private static final double LEVER_PLACE_RANGE = 4.5D;
    private static final float LEVER_PLACE_PITCH = 75.0F;
    private double flowerStartY = 0;
    private boolean isFlowerClimbing = false;
    private static final Direction[] LEVER_WALL_DIRECTIONS = new Direction[]{
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };
    private static final int[] LEVER_PLACE_OFFSETS = new int[]{0, 1, 2};

    @AllArgsConstructor
    private enum Mode implements ModeSetting.NamedChoice {
        FtCollision("Matrix"),
        Levers("ФанТайм Кнопки"),
        Flowers("Цветы"),
        Water("Water Bucket"),
        FunSky("FunSky"),
        SlimeBlock("Slime Block"),
        FunTimeFly("FunTime Fly");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    private final ModeSetting mode = new ModeSetting("Режим").values(Mode.values()).value(Mode.FtCollision);
    private final TimerUtil timerUtil = new TimerUtil();
    private final TimerUtil matrixTimer = new TimerUtil();
    private final TimerUtil ftCollisionTimer = new TimerUtil();
    private final TimerUtil leverPlaceTimer = new TimerUtil();
    private long ftCollisionMicroJumpAt = 0L;
    private long ftCollisionJumpReleaseAt = 0L;
    private boolean ftCollisionHadWallContact = false;
    private boolean ftCollisionForcingJump = false;
    private boolean ftCollisionMicroJumpTriggered = false;
    private boolean leverMissingWarned = false;
    private float leverPitch = 0.0F;

    private float[] leverRotCurrent = new float[2];
    private float[] leverRotStart = new float[2];
    private float[] leverRotTarget = new float[2];
    private long leverRotStartTime = 0L;
    private boolean leverRotating = false;
    private boolean leverRotReturning = false;
    private boolean leverIsDisabling = false;
    private boolean diagonalPhase = true;
    private boolean goRight = false;
    private static final long LEVER_ROT_DURATION = 220L;

    private long lastCollisionTime = 0L;
    private long jumpAfterClimbTime = 0L;

    private boolean climbing = false;
    private int prevSlot = -1;
    private long lastUseTime = 0L;
    private long lastGroundTime = System.currentTimeMillis();
    private int waterSlot = -1;
    private int originalSlot = -1;
    private int movedFromInvSlot = -1;
    private int slimeBoostTick = 0;

    public SpiderModule() {
        addSettings(mode);
    }

    @Override
    public void onEnable() {
        timerUtil.reset();
        matrixTimer.reset();
        ftCollisionTimer.reset();
        leverPlaceTimer.reset();
        ftCollisionMicroJumpAt = 0L;
        ftCollisionJumpReleaseAt = 0L;
        ftCollisionHadWallContact = false;
        ftCollisionForcingJump = false;
        ftCollisionMicroJumpTriggered = false;
        leverMissingWarned = false;
        leverPitch = mc.player != null ? mc.player.getPitch() : 0.0F;
        leverRotating = false;
        leverRotReturning = false;
        if (mc.player != null) {
            leverRotCurrent[0] = mc.player.getYaw();
            leverRotCurrent[1] = mc.player.getPitch();
        }
        lastCollisionTime = 0L;
        jumpAfterClimbTime = 0L;
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is(Mode.Water)) handleWaterMode();
        else if (mode.is(Mode.FunSky)) handleFunSkyMode();
        else if (mode.is(Mode.SlimeBlock)) handleSlimeBlockMode();
        else if (mode.is(Mode.FunTimeFly)) handleFunTimeFlyMode();
    }

    private int findRequiredFlowerSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block instanceof net.minecraft.block.FlowerBlock || block instanceof net.minecraft.block.TallFlowerBlock) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private final Random random = new Random();

    private void handleFlowersMode(MotionEvent event) {
        boolean hasCollision = mc.player.horizontalCollision;
        boolean isMovingForward = mc.options.forwardKey.isPressed();

        if (hasCollision && isMovingForward) {
            lastCollisionTime = System.currentTimeMillis();
            jumpAfterClimbTime = 0L;
            mc.options.jumpKey.setPressed(true);
        } else if (lastCollisionTime != 0L) {
            if (jumpAfterClimbTime == 0L) {
                jumpAfterClimbTime = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - jumpAfterClimbTime < 300L) {
                mc.options.jumpKey.setPressed(true);
            } else {
                mc.options.jumpKey.setPressed(false);
                jumpAfterClimbTime = 0L;
                lastCollisionTime = 0L;
                if (leverRotating) startLeverReturn();
                isFlowerClimbing = false;
                diagonalPhase = true;
                return;
            }
        }

        if (!hasCollision || !isMovingForward) return;

        if (!isFlowerClimbing) {
            flowerStartY = mc.player.getY();
            isFlowerClimbing = true;
            diagonalPhase = true;
            goRight = random.nextBoolean();
        }

        BlockPos targetPos = BlockPos.ofFloored(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ()
        );
        Vec3d offset = getFlowerOffset(targetPos);

        double targetX = targetPos.getX() + 0.5 + offset.x;
        double targetZ = targetPos.getZ() + 0.5 + offset.z;

        float lookYaw = (float) Math.toDegrees(
                Math.atan2(-(targetX - mc.player.getX()), targetZ - mc.player.getZ())
        );

        double climbedY = mc.player.getY() - flowerStartY;
        float finalYaw;

        if (diagonalPhase && climbedY < 2.0) {
            float offset2 = 30.0f + random.nextFloat() * 15.0f;
            finalYaw = lookYaw + (goRight ? offset2 : -offset2);
        } else {
            diagonalPhase = false;
            finalYaw = lookYaw;
        }

        if (mc.player.getY() - flowerStartY < 3.5) {
            if (!leverRotating && !leverRotReturning) {
                startLeverRotateTo(finalYaw, LEVER_PLACE_PITCH);
            } else {
                leverRotTarget[0] = finalYaw;
            }
        }

        long speedMs = (long) (1000.0 / 10);
        if (!matrixTimer.finished(speedMs)) return;
        matrixTimer.reset();

        int slot = findRequiredFlowerSlot();
        if (slot == -1) return;

        event.ground(true);
        mc.player.setOnGround(true);
        mc.player.fallDistance = 0;

        placeBlockAtFeet(slot);
        mc.options.jumpKey.setPressed(true);
    }

    private void handleWaterMode() {
        if (mc.player.isSubmergedInWater() || mc.player.isTouchingWater()) {
            Vec3d currentVelocity = mc.player.getVelocity();
            mc.player.setVelocity(currentVelocity.x, 0.46, currentVelocity.z);
            return;
        }

        if (mc.player.isOnGround()) {
            lastGroundTime = System.currentTimeMillis();
            return;
        }

        long airTime = System.currentTimeMillis() - lastGroundTime;
        if (airTime < 120L) return;

        if (!mc.player.horizontalCollision) {
            mc.options.sneakKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            return;
        }

        int slot = findWaterBucket();
        if (slot == -1) return;

        long cooldown = getCooldown();
        long now = System.currentTimeMillis();
        if (now - lastUseTime < cooldown) return;

        mc.options.jumpKey.setPressed(true);
        prevSlot = mc.player.getInventory().selectedSlot;
        InventoryUtil.swapToSlot(slot);
        InventoryUtil.useItem(Hand.MAIN_HAND);
        Vec3d currentVelocity = mc.player.getVelocity();
        mc.player.setVelocity(currentVelocity.x, 0.45, currentVelocity.z);
        InventoryUtil.swapToSlot(prevSlot);
        mc.options.sneakKey.setPressed(true);
        lastUseTime = now;
    }

    private void handleFunSkyMode() {
        if (!timerUtil.finished(310L)) return;

        if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {
            BlockHitResult hit = raycastFromRotation(mc.player.getYaw(), 80.0f, 4.5);
            if (hit.getType() == HitResult.Type.BLOCK) {
                mc.player.networkHandler.sendPacket(
                        new PlayerInteractItemC2SPacket(
                                Hand.MAIN_HAND,
                                mc.player.getInventory().selectedSlot,
                                mc.player.getYaw(),
                                mc.player.getPitch()
                        )
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            }
        }

        timerUtil.reset();
    }

    private void handleSlimeBlockMode() {
        BlockPos pos = mc.player.getBlockPos();
        BlockPos[] around = new BlockPos[]{pos.east(), pos.west(), pos.north(), pos.south()};

        boolean hasSlimeNearby = false;
        for (BlockPos checkPos : around) {
            if (mc.world.getBlockState(checkPos).getBlock() == Blocks.SLIME_BLOCK) {
                hasSlimeNearby = true;
                break;
            }
        }

        if (!hasSlimeNearby || !mc.player.horizontalCollision || mc.player.getVelocity().y <= -1.0) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;
        if (mc.world.getBlockState(blockHit.getBlockPos()).isAir()) return;

        int slimeSlot = findHotbarSlot(item -> item == Items.SLIME_BLOCK);
        if (slimeSlot == -1) return;

        int previousSlot = mc.player.getInventory().selectedSlot;
        InventoryUtil.swapToSlot(slimeSlot);
        mc.player.setPitch(54.0f);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
        mc.player.swingHand(Hand.MAIN_HAND);
        InventoryUtil.swapToSlot(previousSlot);

        if (slimeBoostTick >= 1) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.63, mc.player.getVelocity().z);
            slimeBoostTick = 0;
        } else {
            slimeBoostTick++;
        }
    }

    private void handleFunTimeFlyMode() {
        if (!mc.player.horizontalCollision) return;
        if (!timerUtil.finished(1L)) return;

        mc.player.setOnGround(true);
        mc.player.jump();

        int lightningRodSlot = findLightningRodSlot();
        if (lightningRodSlot != -1) {
            placeLightningRodsAbove(lightningRodSlot);
            mc.player.fallDistance = 0.0f;
            timerUtil.reset();
        }
    }

    private int findWaterBucket() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    private long getCooldown() {
        double dist = distanceToGround();
        return dist <= 9.9 ? 450L : 1L;
    }

    private double distanceToGround() {
        double y = mc.player.getY();
        for (double d = y; d > 0; d -= 0.1) {
            BlockPos pos = new BlockPos((int) mc.player.getX(), (int) d, (int) mc.player.getZ());
            if (!mc.world.isAir(pos)) {
                return y - (d + 1.0);
            }
        }
        return 0;
    }

    private Vec3d getFlowerOffset(BlockPos pos) {
        if (mc.world == null) return Vec3d.ZERO;
        return mc.world.getBlockState(pos).getModelOffset(pos);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is(Mode.FtCollision)) handleFtCollisionMode(event);
        else if (mode.is(Mode.Flowers)) handleFlowersMode(event);
        else if (mode.is(Mode.Levers)) handleLeversMode(event);
    }

    @EventHandler
    public void onJump(JumpEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is(Mode.FtCollision) || mode.is(Mode.Levers)) {
            ftCollisionTimer.reset();
        }
    }

    private void handleLeversMode(MotionEvent event) {
        handleLeverRotation();
        if (leverIsDisabling) return;

        boolean active = mc.options.forwardKey.isPressed() && mc.player.horizontalCollision;

        if (!active) {
            if (leverRotating) startLeverReturn();
            mc.options.jumpKey.setPressed(false);
            return;
        }

        if (!leverRotating && !leverRotReturning) {
            startLeverRotateTo(mc.player.getYaw(), LEVER_PLACE_PITCH);
        }

        mc.options.jumpKey.setPressed(true);

        if (!leverPlaceTimer.finished(LEVER_PLACE_DELAY_MS)) {
            return;
        }
        mc.player.setOnGround(true);
        event.ground(true);
        mc.options.jumpKey.setPressed(true);
        mc.player.fallDistance = 0.0F;

        int slot = findRequiredFunTimeSlot();
        if (slot == -1) {
            if (!leverMissingWarned) {
                print("Нужны кнопки");
                leverMissingWarned = true;
            }
            setEnabled(false);
            return;
        }
        leverMissingWarned = false;
        placeBlockAtFeet(slot);
        leverPlaceTimer.reset();
    }

    private void startLeverRotateTo(float yaw, float pitch) {
        leverRotStart[0] = leverRotCurrent[0];
        leverRotStart[1] = leverRotCurrent[1];
        leverRotTarget[0] = yaw;
        leverRotTarget[1] = pitch;
        leverRotStartTime = System.currentTimeMillis();
        leverRotating = true;
        leverRotReturning = false;
        applyLeverRotation();
    }

    private void startLeverReturn() {
        leverRotStart[0] = leverRotCurrent[0];
        leverRotStart[1] = leverRotCurrent[1];
        leverRotTarget[0] = mc.player.getYaw();
        leverRotTarget[1] = mc.player.getPitch();
        leverRotStartTime = System.currentTimeMillis();
        leverRotating = false;
        leverRotReturning = true;
        applyLeverRotation();
    }

    private void handleLeverRotation() {
        if (!leverRotating && !leverRotReturning) return;

        long elapsed = System.currentTimeMillis() - leverRotStartTime;
        float progress = Math.min(1.0f, (float) elapsed / LEVER_ROT_DURATION);
        float smooth = easeInOutCubic(progress);

        leverRotCurrent[0] = interpolateAngle(leverRotStart[0], leverRotTarget[0], smooth);
        leverRotCurrent[1] = leverRotStart[1] + (leverRotTarget[1] - leverRotStart[1]) * smooth;

        float gcd = gcdSafe();
        Rotation serverRot = dev.ethereal.api.utils.rotation.manager.RotationManager.getInstance().getServerRotation();
        float sYaw = serverRot != null ? serverRot.getYaw() : mc.player.getYaw();
        float sPitch = serverRot != null ? serverRot.getPitch() : mc.player.getPitch();
        leverRotCurrent[0] = mc.player.getYaw();
        float pitchDiff = leverRotCurrent[1] - sPitch;
        leverRotCurrent[1] = MathHelper.clamp(sPitch + Math.round(pitchDiff / gcd) * gcd, -89.0F, 89.0F);

        applyLeverRotation();

        if (progress >= 1.0f) {
            if (leverRotReturning) {
                RotationComponent.getInstance().stopRotation();
                leverRotReturning = false;
                if (leverIsDisabling) {
                    resetLeverRotState();
                    doActualDisable();
                }
            } else {
                leverRotating = true;
            }
        }
    }

    private void applyLeverRotation() {
        RotationComponent.update(
                new Rotation(leverRotCurrent[0], leverRotCurrent[1]),
                180f, 180f, 180f, 180f, 5, 50, false
        );
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (-2 * t + 2) * (-2 * t + 2) * (-2 * t + 2) / 2;
    }

    private float interpolateAngle(float start, float end, float progress) {
        float diff = MathHelper.wrapDegrees(end - start);
        return start + diff * progress;
    }

    private float gcdSafe() {
        double s = mc.options.getMouseSensitivity().getValue();
        double f = s * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }

    private void placeBlockAtFeet(int slot) {
        if (mc.currentScreen != null || mc.interactionManager == null) return;

        InventoryUtil.swapToSlot(slot);
        BlockHitResult hit = raycastFromRotation(mc.player.getYaw(), LEVER_PLACE_PITCH, LEVER_PLACE_RANGE);
        if (isValidLeverPlaceHit(hit)) {
            placeLever(hit);
            return;
        }

        Direction wallDirection = findLeverWallDirection();
        if (wallDirection == null) return;

        for (int offset : LEVER_PLACE_OFFSETS) {
            LeverPlaceTarget target = getLeverPlaceTarget(wallDirection, offset);
            if (target == null) continue;

            Rotation rot = RotationUtil.rotationAt(target.hitVec);
            RotationComponent.update(rot, 180f, 180f, 180f, 180f, 2, 50, false);
            placeLever(target.hitResult);
            return;
        }
    }

    private void placeLever(BlockHitResult hitResult) {
        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        if (result.isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private Direction findLeverWallDirection() {
        BlockPos basePos = mc.player.getBlockPos();
        Direction closestDirection = null;
        double closestDistance = Double.MAX_VALUE;

        for (Direction direction : LEVER_WALL_DIRECTIONS) {
            for (int offset = 0; offset <= 1; offset++) {
                BlockPos supportPos = basePos.up(offset).offset(direction);
                if (!isLeverSupport(supportPos)) continue;

                double distance = mc.player.getPos().squaredDistanceTo(supportPos.toCenterPos());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestDirection = direction;
                }
            }
        }

        return closestDirection;
    }

    private boolean isValidLeverPlaceHit(BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return false;
        Direction side = hit.getSide();
        if (side == Direction.UP || side == Direction.DOWN) return false;

        BlockPos supportPos = hit.getBlockPos();
        BlockPos placePos = supportPos.offset(side);
        if (!isLeverSupport(supportPos)) return false;
        if (!mc.world.getBlockState(placePos).isReplaceable()) return false;

        return mc.player.getEyePos().squaredDistanceTo(hit.getPos()) <= LEVER_PLACE_RANGE * LEVER_PLACE_RANGE;
    }

    private LeverPlaceTarget getLeverPlaceTarget(Direction wallDirection, int yOffset) {
        BlockPos playerPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() + yOffset, mc.player.getZ());
        BlockPos supportPos = playerPos.offset(wallDirection);
        Direction side = wallDirection.getOpposite();
        BlockPos placePos = supportPos.offset(side);

        if (!isLeverSupport(supportPos)) return null;
        if (!mc.world.getBlockState(placePos).isReplaceable()) return null;
        if (mc.player.getEyePos().squaredDistanceTo(placePos.toCenterPos()) > LEVER_PLACE_RANGE * LEVER_PLACE_RANGE) return null;

        Vec3d hitVec = supportPos.toCenterPos().add(
                side.getOffsetX() * 0.5D,
                0.0D,
                side.getOffsetZ() * 0.5D
        );
        return new LeverPlaceTarget(hitVec, new BlockHitResult(hitVec, side, supportPos, false));
    }

    private boolean isLeverSupport(BlockPos pos) {
        return !mc.world.getBlockState(pos).isAir()
                && !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    private int findRequiredFunTimeSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ButtonBlock) return slot;
        }
        return -1;
    }

    private void handleFtCollisionMode(MotionEvent event) {
        boolean physicalJumpPressed = isPhysicalJumpPressed();
        boolean hasWallCollision = hasAnySideCollision();
        long now = System.currentTimeMillis();

        if (hasWallCollision && !ftCollisionHadWallContact) {
            ftCollisionMicroJumpAt = now + FT_COLLISION_MICRO_JUMP_DELAY_MS;
            ftCollisionJumpReleaseAt = 0L;
            ftCollisionMicroJumpTriggered = false;
        }

        if (hasWallCollision) {
            ftCollisionHadWallContact = true;
            holdFtCollisionJump();
        } else if (ftCollisionHadWallContact) {
            ftCollisionHadWallContact = false;
            ftCollisionTimer.reset();
            ftCollisionMicroJumpAt = 0L;
            ftCollisionMicroJumpTriggered = false;
            ftCollisionJumpReleaseAt = now + FT_COLLISION_JUMP_RELEASE_BUFFER_MS;
        }

        if (!hasWallCollision) {
            if (ftCollisionForcingJump) {
                if (ftCollisionJumpReleaseAt != 0L && now < ftCollisionJumpReleaseAt) {
                    mc.options.jumpKey.setPressed(true);
                } else {
                    restoreFtCollisionJump(physicalJumpPressed);
                    ftCollisionJumpReleaseAt = 0L;
                }
            }
            return;
        }

        ftCollisionJumpReleaseAt = 0L;

        boolean boostedThisTick = false;
        if (!ftCollisionMicroJumpTriggered && ftCollisionMicroJumpAt != 0L && now >= ftCollisionMicroJumpAt) {
            ftCollisionMicroJumpTriggered = true;
            applyFtCollisionBoost(event);
            ftCollisionTimer.reset();
            boostedThisTick = true;
        }

        if (!boostedThisTick && ftCollisionTimer.finished(FT_COLLISION_DELAY_MS)) {
            applyFtCollisionBoost(event);
            ftCollisionTimer.reset();
        }
    }

    private void applyFtCollisionBoost(MotionEvent event) {
        event.ground(true);
        mc.player.setOnGround(true);
        mc.player.fallDistance = 0.0f;
        mc.player.jump();

        Vec3d velocity = mc.player.getVelocity();
        if (velocity.y < FT_COLLISION_VERTICAL_BOOST) {
            mc.player.setVelocity(velocity.x, FT_COLLISION_VERTICAL_BOOST, velocity.z);
        }
    }

    private void holdFtCollisionJump() {
        mc.options.jumpKey.setPressed(true);
        ftCollisionForcingJump = true;
    }

    private void restoreFtCollisionJump(boolean physicalJumpPressed) {
        if (!ftCollisionForcingJump) {
            return;
        }

        mc.options.jumpKey.setPressed(physicalJumpPressed);
        ftCollisionForcingJump = false;
    }

    private boolean isPhysicalJumpPressed() {
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.jumpKey.getDefaultKey().getCode());
    }

    private void placeLightningRodsAbove(int slot) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        InventoryUtil.swapToSlot(slot);

        BlockPos base = mc.player.getBlockPos();
        for (int y = 1; y <= 2; y++) {
            BlockPos placePos = base.up(y);
            if (mc.world.getBlockState(placePos).isAir()) {
                placeLightningRodAt(placePos);
            }
        }

        InventoryUtil.swapToSlot(previousSlot);
    }

    private void placeLightningRodAt(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return;

        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos.down(), false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int findLightningRodSlot() {
        return findHotbarSlot(item -> item == Items.LIGHTNING_ROD);
    }

    private int findHotbarSlot(Predicate<Item> predicate) {
        for (int slot = 0; slot < 9; slot++) {
            Item item = mc.player.getInventory().getStack(slot).getItem();
            if (predicate.test(item)) return slot;
        }
        return -1;
    }

    private BlockHitResult raycastFromRotation(float yaw, float pitch, double range) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d dir = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d end = eyePos.add(dir.multiply(range));

        RaycastContext context = new RaycastContext(
                eyePos,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );

        return mc.world.raycast(context);
    }

    private boolean hasAnySideCollision() {
        if (mc.player.horizontalCollision) {
            return true;
        }

        Box playerBox = mc.player.getBoundingBox();
        Box sideCheckBox = new Box(
                playerBox.minX - FT_COLLISION_CLIMB_MOTION,
                playerBox.minY + FT_COLLISION_CLIMB_MOTION,
                playerBox.minZ - FT_COLLISION_CLIMB_MOTION,
                playerBox.maxX + FT_COLLISION_CLIMB_MOTION,
                playerBox.maxY - FT_COLLISION_CLIMB_MOTION,
                playerBox.maxZ + FT_COLLISION_CLIMB_MOTION
        );

        return BlockPos.stream(sideCheckBox).anyMatch(pos -> hasSideCollisionAt(sideCheckBox, pos));
    }

    private boolean hasSideCollisionAt(Box sideCheckBox, BlockPos pos) {
        VoxelShape shape = mc.world.getBlockState(pos).getCollisionShape(mc.world, pos);
        if (shape.isEmpty()) {
            return false;
        }

        return shape.getBoundingBoxes().stream()
                .map(box -> box.offset(pos.getX(), pos.getY(), pos.getZ()))
                .anyMatch(sideCheckBox::intersects);
    }

    @Override
    public void onDisable() {
        resetLeverRotState();
        doActualDisable();
    }

    private void doActualDisable() {
        mc.options.sneakKey.setPressed(false);
        lastCollisionTime = 0L;
        jumpAfterClimbTime = 0L;
        mc.options.jumpKey.setPressed(isPhysicalJumpPressed());
        ftCollisionMicroJumpAt = 0L;
        ftCollisionJumpReleaseAt = 0L;
        ftCollisionHadWallContact = false;
        ftCollisionForcingJump = false;
        ftCollisionMicroJumpTriggered = false;
        leverMissingWarned = false;
        leverPitch = 0.0F;
        prevSlot = -1;
        lastUseTime = 0L;
        slimeBoostTick = 0;
        timerUtil.reset();
        matrixTimer.reset();
        ftCollisionTimer.reset();
        leverPlaceTimer.reset();
    }

    private void resetLeverRotState() {
        RotationComponent.getInstance().stopRotation();
        leverRotating = false;
        leverRotReturning = false;
        leverIsDisabling = false;
    }

    private void stopClimbing() {
        if (!climbing) return;
        climbing = false;

        if (originalSlot != -1 && mc.player != null) {
            sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
            originalSlot = -1;
        }

        if (waterSlot != -1 && movedFromInvSlot != -1 && mc.player != null) {
            InventoryUtil.swapSlots(waterSlot, movedFromInvSlot);
        }

        waterSlot = -1;
        movedFromInvSlot = -1;
    }

    private record LeverPlaceTarget(Vec3d hitVec, BlockHitResult hitResult) {
    }
}
