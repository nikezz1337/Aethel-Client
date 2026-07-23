package dev.aethel.module.list.combat.aura;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.math.BlockPos;

public class TralaleloTralala {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static boolean jumped;

    public static boolean isJumped() { return jumped; }
    public static void setJumped(boolean j) { jumped = j; }

    public static float getAICooldown() {
        if (mc.player.getMainHandStack().getItem() == Items.AIR) {
            return 1;
        }

        if (mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING)
                || mc.player.isInLava()
                || mc.player.isTouchingWater()
                || mc.player.isClimbing()
                || mc.player.hasVehicle()
                || isPlayerInWeb()
                || mc.player.isGliding()
                || mc.player.getAbilities().flying)
            return 0.944f;

        if (mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof ShovelItem)
            return 0.99f;

        return 0.944f;
    }

    public static float getNewFallDistance(LivingEntity target) {
        return 0;
    }

    public static boolean canAIFall() {
        BlockState b1 = getBlock(0, 3, 0);
        BlockState b2 = getBlock(0, 2, 0);
        BlockState b3 = getBlock(0, 1, 0);
        return ((b1.isOf(Blocks.AIR) && b2.isOf(Blocks.AIR) && b3.isOf(Blocks.AIR))
                || mc.player.fallDistance < (!b2.isOf(Blocks.AIR) ? 0.08f : 0.6f)
                || mc.player.fallDistance > 1.2f);
    }

    private static boolean isAir(double x, double y, double z) {
        return mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).isOf(Blocks.AIR);
    }

    private static boolean isPlayerInWeb() {
        if (mc.player == null || mc.world == null) return false;
        BlockPos pos = mc.player.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        return state.isOf(Blocks.COBWEB) || state.isOf(Blocks.POWDER_SNOW);
    }

    private static BlockState getBlock(int x, int y, int z) {
        if (mc.player == null || mc.world == null) return Blocks.AIR.getDefaultState();
        return mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(x, y, z)));
    }
}
