package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.aura.rotation.Rotation;
import dev.aethel.module.list.combat.aura.rotation.URotations;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(
    moduleName = "ClanUpgrader",
    moduleCategory = ModuleCategory.PLAYER,
    moduleDesc = "Авто-апгрейд клана"
)
public class ClanUpgrader extends Module {

    private int slot = -1;

    @Override
    public void onEnable() {
        super.onEnable();
        slot = findRedstoneInHotbar();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventPlayerUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (slot == -1) {
            slot = findRedstoneInHotbar();
            if (slot == -1) return;
        }

        mc.player.getInventory().selectedSlot = slot;

        URotations.update(new Rotation(0, 90), 360, 360, 360, 360, 2, 0, false);

        BlockPos targetPos = mc.player.getBlockPos();
        BlockState state = mc.world.getBlockState(targetPos);

        if (state.isAir()) {
            BlockPos below = targetPos.down();
            if (mc.world.getBlockState(below).isSolid()) {
                BlockHitResult hit = new BlockHitResult(
                        new Vec3d(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5),
                        Direction.UP, below, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else if (state.getBlock() == Blocks.REDSTONE_WIRE) {
            mc.interactionManager.attackBlock(targetPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private int findRedstoneInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.REDSTONE) return i;
        }
        return -1;
    }
}
