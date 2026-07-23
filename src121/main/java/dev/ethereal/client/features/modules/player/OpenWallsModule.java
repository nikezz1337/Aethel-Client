package dev.ethereal.client.features.modules.player;

import net.minecraft.block.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;

import lombok.Getter;

@ModuleRegister(name = "Open Walls", category = Category.PLAYER)
public class OpenWallsModule extends Module {
    @Getter private static final OpenWallsModule instance = new OpenWallsModule();

    private final SliderSetting distance = new SliderSetting("Дистанция").value(5f).range(1f, 6f).step(0.5f);
    private boolean wasRightClicked;

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        boolean rightClick = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (rightClick && !wasRightClicked) {
            openNearestBlock();
        }

        wasRightClicked = rightClick;
    }

    private void openNearestBlock() {
        float maxDist = distance.getValue();
        BlockPos best = null;
        double bestDot = -Double.MAX_VALUE;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);

        int r = MathHelper.ceil(maxDist);
        BlockPos origin = BlockPos.ofFloored(eyePos);

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    double dist = eyePos.distanceTo(Vec3d.ofCenter(pos));
                    if (dist > maxDist) continue;

                    if (isInteractive(mc.world.getBlockState(pos).getBlock())) {
                        Vec3d toBlock = Vec3d.ofCenter(pos).subtract(eyePos).normalize();
                        double dot = lookVec.dotProduct(toBlock);
                        if (dot > bestDot) {
                            bestDot = dot;
                            best = pos;
                        }
                    }
                }
            }
        }

        if (best == null) return;

        Vec3d blockCenter = Vec3d.ofCenter(best);
        Direction face = Direction.getFacing(
                blockCenter.x - eyePos.x,
                blockCenter.y - eyePos.y,
                blockCenter.z - eyePos.z
        );

        BlockHitResult hitResult = new BlockHitResult(blockCenter, face, best, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
    }

    private boolean isInteractive(Block block) {
        return block instanceof ChestBlock
                || block instanceof TrappedChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof EnderChestBlock
                || block instanceof FurnaceBlock
                || block instanceof BlastFurnaceBlock
                || block instanceof SmokerBlock
                || block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof GrindstoneBlock
                || block instanceof CartographyTableBlock
                || block instanceof LoomBlock
                || block instanceof SmithingTableBlock
                || block instanceof StonecutterBlock
                || block instanceof EnchantingTableBlock
                || block instanceof BeaconBlock
                || block instanceof BrewingStandBlock
                || block instanceof HopperBlock
                || block instanceof BedBlock
                || block instanceof CommandBlock
                || block instanceof DropperBlock;
    }
}
