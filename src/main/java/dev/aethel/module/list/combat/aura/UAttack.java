package dev.aethel.module.list.combat.aura;

import dev.aethel.event.list.EventAttack;
import dev.aethel.module.list.combat.Aura;
import dev.aethel.util.render.math.MathUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class UAttack {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static long hitCounterCPSBypass;

    public static void hitCounterCPSBypassNext() { ++hitCounterCPSBypass; }
    public static void hitCounterCPSBypassReset() { hitCounterCPSBypass = 0; }
    public static boolean cpsBypassTrigger() { return hitCounterCPSBypass % 7 == 3; }

    public static PlayerEntity getSelf() { return mc.player; }

    public static float applyGaussianJitter(float rotation) {
        final float strength = .2F;
        return (float) (rotation + (new SecureRandom().nextGaussian() * strength * 2.F - strength));
    }

    public static float randomFloat(float min, float max) {
        return new SecureRandom().nextFloat(min, max);
    }

    public static int getAxeSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().main.get(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    public static Runnable[] hitShieldBreakTaskForUse(LivingEntity livingIn, boolean enabled) {
        final Runnable[] pre$post = new Runnable[]{() -> {}, () -> {}};
        if (!enabled) return pre$post;
        if (livingIn instanceof PlayerEntity player && Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - player.getYaw() - 180)) > 90) {
            final ItemStack main = player.getMainHandStack(), off = player.getOffHandStack();
            if (main != null && off != null) {
                final Item mainItem = main.getItem(), offItem = off.getItem();
                if (mainItem == Items.SHIELD || offItem == Items.SHIELD) {
                    final int slot, handSlot = mc.player.getInventory().selectedSlot;
                    if ((slot = getAxeSlot()) != -1 && slot != handSlot) {
                        final int finalSlot = slot;
                        pre$post[0] = () -> mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(finalSlot));
                        pre$post[1] = () -> mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(handSlot));
                    }
                }
            }
        }
        return pre$post;
    }

    public static Runnable[] resetShieldSilentTaskForUse(boolean enabled) {
        final Runnable[] pre$post = new Runnable[]{() -> {}, () -> {}};
        if (!enabled) return pre$post;
        if (mc.player.isBlocking()) {
            Hand active = mc.player.getActiveHand();
            if (active == null) return pre$post;
            pre$post[0] = () -> mc.player.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            pre$post[1] = () -> mc.player.networkHandler.sendPacket(
                    new HandSwingC2SPacket(active));
        }
        return pre$post;
    }

    public static Runnable[] skipSilentSprintingTaskForUse(boolean enabled) {
        final Runnable[] pre$post = new Runnable[]{() -> {}, () -> {}};
        if (!enabled) return pre$post;
        if (mc.player.isSprinting() && !mc.player.isOnGround() && !mc.player.isTouchingWater()) {
            pre$post[0] = () -> mc.player.setSprinting(false);
            pre$post[1] = () -> mc.player.setSprinting(true);
        }
        return pre$post;
    }

    public static double getYCapacityOnPlayerPos(int rangeY) {
        if (mc.world == null) return 1.D;
        return 3.D;
    }

    public static double convenientFallOffset() {
        double fallOffset = mc.player.fallDistance;
        if (mc.world != null && !mc.player.isOnGround() && mc.player.getVelocity().y < -.0784000015258789D
                && mc.world.getBlockState(mc.player.getBlockPos()).getFluidState().isEmpty()
                && mc.world.getBlockState(mc.player.getBlockPos().up()).getFluidState().isEmpty()) {
            if (mc.player.fallDistance < -mc.player.getVelocity().y)
                fallOffset = -mc.player.getVelocity().y;
        }
        return fallOffset;
    }

    private static final StopWatch cooldownTimer = new StopWatch();

    public static long getMsCooldown() {
        long msCooldown;
        double attributeAttackSpeed = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
        float maxDeviation = .2F;
        msCooldown = (long) ((1.F / attributeAttackSpeed) * 1000.F * (1.F - Math.min(maxDeviation, 1.F)));

        if (attributeAttackSpeed == 4.D || attributeAttackSpeed == 4.4000000059604645 || attributeAttackSpeed == 4.800000011920929)
            msCooldown = 450L;

        msCooldown = Math.max(msCooldown, 500L);

        if (Aura.getInstance().attackDelay.is("Динамичный")) {
            msCooldown = ThreadLocalRandom.current().nextLong(350, 650);
        } else if (Aura.getInstance().attackDelay.is("Статичный")) {
            msCooldown = 500;
        } else if (Aura.getInstance().attackDelay.is("Быстрый")) {
            msCooldown = (long) (765.0 / attributeAttackSpeed + 10.0);
            msCooldown = Math.max(msCooldown, 100L);
        } else if (Aura.getInstance().attackDelay.is("1.8")) {
            msCooldown = 100;
        } else {
            if (cpsBypassTrigger()) msCooldown += 150L;
        }
        return msCooldown;
    }

    public static boolean msCooldownReached(long msOffset) {
        return cooldownTimer.finished(getMsCooldown() + msOffset);
    }

    public static boolean msCooldownReached() { return msCooldownReached(0); }

    public static boolean msCooldownHasMs(long ms) { return cooldownTimer.finished(ms); }

    public static boolean isBestMomentToHit(boolean fallCheck) {
        if (!fallCheck) return true;
        float adaptiveFallValue = Aura.getInstance().attackDelay.is("Динамичный")
                ? MathUtil.random(0.0F, 0.2F)
                : 0.F;

        final boolean hasFall = convenientFallOffset() > adaptiveFallValue || getYCapacityOnPlayerPos(2) < .1F;
        if (hasFall) return true;
        final boolean badLiquidMoment =
                !mc.player.input.playerInput.jump() && (mc.player.isTouchingWater() || mc.player.isInLava())
                        || mc.player.isTouchingWater() || mc.player.isInLava();
        final boolean skipFallCheck =
                badLiquidMoment
                        || (!mc.player.input.playerInput.jump() && mc.player.isOnGround()) && Aura.getInstance().onlyCrits.getValue()
                        || mc.player.isClimbing()
                        || mc.player.isGliding()
                        || mc.player.hasVehicle()
                        || mc.player.isSwimming()
                        || mc.player.isSubmergedInWater()
                        || hasCobwebAtPlayer()
                        || mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                        || mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.LEVITATION)
                        || mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING)
                        || mc.player.getAbilities().flying
                        || mc.player.getY() < -64.D;
        return skipFallCheck;
    }

    private static boolean hasCobwebAtPlayer() {
        if (mc.player == null || mc.world == null) return false;
        BlockPos pos = mc.player.getBlockPos();
        return mc.world.getBlockState(pos).isOf(Blocks.COBWEB)
                || mc.world.getBlockState(pos.up()).isOf(Blocks.COBWEB);
    }

    public static boolean anyEntityOnRay(LivingEntity livingIn, double range) {
        return livingIn != null && RayTraceUtil.isViewEntity(livingIn,
                MathHelper.wrapDegrees(mc.player.getYaw()),
                mc.player.getPitch(), (float) range, true);
    }

    public static boolean shouldAttack(LivingEntity livingTarget, boolean rayCast, boolean distanceCheck, boolean fallCheck, long cooldownMSOffset, float[] ranges) {
        if (distanceCheck && livingTarget != null && mc.player.distanceTo(livingTarget) > ranges[0])
            return false;
        if (!msCooldownReached(cooldownMSOffset)) return false;
        boolean validNext = isBestMomentToHit(fallCheck);
        if (validNext && rayCast && !anyEntityOnRay(livingTarget, ranges[0]))
            validNext = false;
        return validNext;
    }

    public static boolean shouldAttack(LivingEntity livingTarget, boolean rayCast, boolean fallCheck, long cooldownMSOffset, float[] ranges) {
        return shouldAttack(livingTarget, rayCast, true, fallCheck, cooldownMSOffset, ranges);
    }

    public static boolean resetSprintTick(LivingEntity targetIn, float[] ranges) {
        if (targetIn != null && shouldAttack(targetIn, false, false, -60L, ranges)
                && !mc.player.isOnGround() && !mc.player.isTouchingWater()
                && mc.player.getVelocity().y <= 0.0030162615090425808) {
            return true;
        }
        return false;
    }

    public static boolean useEntity(LivingEntity livingIn, Runnable preHit, Runnable postHit, Hand hand, boolean cpsBypass) {
        if (!msCooldownReached()) return false;

        if (preHit != null) preHit.run();

        if (livingIn != null) {
            new EventAttack(livingIn).post();
            mc.interactionManager.attackEntity(mc.player, livingIn);

            if (cpsBypass) {
                hitCounterCPSBypassNext();
            } else {
                hitCounterCPSBypassReset();
            }

            cooldownTimer.reset();
            if (hand != null) mc.player.swingHand(hand);

            return true;
        }

        if (postHit != null) postHit.run();
        return false;
    }
}
