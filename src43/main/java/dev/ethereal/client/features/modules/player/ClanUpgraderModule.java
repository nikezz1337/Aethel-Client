package dev.ethereal.client.features.modules.player;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.other.StopWatch;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleRegister(name = "Clan Upgrader", category = Category.PLAYER)
@Environment(EnvType.CLIENT)
public class ClanUpgraderModule extends Module {
    @Getter private static final ClanUpgraderModule instance = new ClanUpgraderModule();

    private int oldSlot = -1;
    private int slot = -1;
    private int delay = 0;
    private boolean shouldBreak = false;
    private boolean shouldPlace = false;

    public ClanUpgraderModule() {
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.world == null || mc.player == null) return;

            if (delay > 0) {
                delay--;
                return;
            }

            if (slot == -1) {
                slot = InventoryUtil.findItem(Items.REDSTONE, true);
                if (slot == -1) slot = InventoryUtil.findItem(Items.TORCH, true);
                if (slot == -1) return;
            }
      
            RotationComponent.update(new Rotation(mc.player.getYaw(), 90),
                    360, 360,
                    90, 90,
                    0, 1, false);

            BlockPos blockBelow = mc.player.getBlockPos().down();
            Block currentBlock = mc.world.getBlockState(blockBelow).getBlock();

            if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
                mc.options.jumpKey.setPressed(true);
            } else if (!mc.player.isOnGround()) {
                mc.options.jumpKey.setPressed(false);
            }

            if (shouldPlace) {
                place(blockBelow);
                shouldPlace = false;
                delay = 1;
            }
            else if (shouldBreak) {
                mc.interactionManager.updateBlockBreakingProgress(blockBelow, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
                shouldBreak = false;
                delay = 1;
            }
            else {
                if (currentBlock == Blocks.AIR) {
                    shouldPlace = true;
                }
                else if (currentBlock == Blocks.REDSTONE_WIRE || currentBlock == Blocks.TORCH) {
                    shouldBreak = true;
                }
                delay = 1;
            }
        }));

        addEvents(tickEvent);
    }

    private void place(BlockPos pos) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (mc.player.getInventory().selectedSlot != slot) {
            if (oldSlot == -1) {
                oldSlot = mc.player.getInventory().selectedSlot;
            }
            mc.player.getInventory().selectedSlot = slot;
        }

        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(pos),
                Direction.UP,
                pos,
                false
        );
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void onDisable() {
        if (mc.player != null && oldSlot >= 0 && oldSlot <= 8) {
            mc.player.getInventory().selectedSlot = oldSlot;
        }
        oldSlot = -1;
        slot = -1;
        delay = 0;
        shouldBreak = false;
        shouldPlace = false;
        super.onDisable();
    }
}
