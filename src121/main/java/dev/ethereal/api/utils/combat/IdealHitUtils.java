package dev.ethereal.api.utils.combat;

import dev.ethereal.api.utils.rotation.manager.RotationManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static dev.ethereal.api.system.interfaces.QuickImports.mc;

public final class IdealHitUtils {

    private IdealHitUtils() {
    }

    public static float getAICooldown() {
        if (mc.player.getMainHandStack().getItem() == Items.AIR) return 1;

        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                || mc.player.isInLava()
                || mc.player.isGliding()
                || mc.player.getAbilities().flying)
            return 0.944f;

        if (mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof ShovelItem) return 0.99f;
        if (mc.player.isGliding()) return 1;

        return 0.944f;
    }

    public static boolean canAIFall() {
        return ((getBlock(0, 3, 0) == Blocks.AIR && getBlock(0, 2, 0) == Blocks.AIR && getBlock(0, 1, 0) == Blocks.AIR)
                || ServerManager.getInstance().getFallDistance() < (getBlock(0, 2, 0) != Blocks.AIR ? 0.08f : 0.6f)
                || ServerManager.getInstance().getFallDistance() > 1.2f);
    }

    public static boolean canCritical(LivingEntity target) {
        double yDiff = (double)((int) mc.player.getY()) - mc.player.getY();
        boolean bl4 = yDiff == -0.01250004768371582;
        boolean bl5 = yDiff == -0.1875;

        return (!mc.player.isOnGround() && ServerManager.getInstance().getFallDistance() > 0f && canAIFall() || target != null && getBlock(0, 2, 0) != Blocks.AIR && getBlock(0, -1, 0) != Blocks.AIR)
                || ((bl5 || bl4) && !mc.player.isSneaking()
                || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                || mc.player.isInLava()
                || mc.player.getAbilities().flying
                || mc.player.isOnGround()
                && !mc.options.jumpKey.isPressed());
    }

    public static boolean isServerReadyForCritical() {
        if (mc.player == null || mc.world == null) return false;

        return !ServerManager.getInstance().isServerOnGround()
                && canAIFall();
    }

    public static boolean shouldSyncCriticalPacket() {
        if (mc.player == null || mc.world == null) return false;

        return !mc.player.isOnGround()
                && ServerManager.getInstance().isServerOnGround()
                && canAIFall();
    }

    public static void syncCriticalPacket(float yaw, float pitch) {
        if (!shouldSyncCriticalPacket()) return;

        RotationManager.getInstance().addPacketRotation(new float[]{yaw, pitch});
    }

    public static Block getBlock(double x, double y, double z) {
        return !(mc.player == null || mc.world == null) ? mc.world.getBlockState(mc.player.getBlockPos().add((int) x, (int) y, (int) z)).getBlock() : Blocks.AIR;
    }

    public static boolean findFall(float fallDistance) {
        Vec3d rotationVec = mc.player.getRotationVector();
        double tempVelocityX = mc.player.getVelocity().x;
        double tempVelocityY = mc.player.getVelocity().y;
        double tempVelocityZ = mc.player.getVelocity().z;

        float n = MathHelper.cos(mc.player.getPitch() * 0.017453292f);
        n = (float) (n * n * Math.min(rotationVec.length() / 0.4, 1.0));

        Vec3d vec3d = new Vec3d(tempVelocityX, tempVelocityY, tempVelocityZ).add(0.0, 0.08 * (-1.0 + n * 0.75), 0.0);
        tempVelocityY = vec3d.y * 0.9800000190734863;

        return tempVelocityY < fallDistance;
    }

    private static boolean hasCriticalRestrictions() {
        return mc.player.isClimbing()
                || mc.player.isTouchingWater()
                || mc.player.isSubmergedInWater()
                || mc.player.isSwimming()
                || mc.player.hasVehicle()
                || mc.player.isGliding()
                || mc.player.getAbilities().flying
                || mc.player.isInLava()
                || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
    }
}
