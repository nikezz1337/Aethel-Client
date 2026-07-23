package dev.aethel.module.list.movement;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventNoSlow;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.ModeSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

@ModuleInformation(
    moduleName = "NoSlow",
    moduleCategory = ModuleCategory.MOVEMENT,
    moduleDesc = "Убирает замедление во время еды"
)
public class NoSlow extends Module {

    public final ModeSetting mode = new ModeSetting("Мод", "Grim New", "Grim New", "Grim old", "Polar");

    @Subscribe
    public void onNoSlow(EventNoSlow e) {
        if (mc.player == null) return;

        switch (mode.getValue()) {
            case "Grim New" -> {
                if (mc.player.getItemUseTime() % 2 == 0) {
                    e.setCancelled(true);
                }
            }
            case "Grim old" -> {
                e.setCancelled(true);
            }
            case "Polar" -> {
                Block below = mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ())).getBlock();
                Block at = mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ())).getBlock();
                boolean onIce = below == Blocks.ICE && mc.player.getMainHandStack().getItem() != Items.TRIDENT;
                boolean inSnow = at == Blocks.SNOW;
                boolean holdingCrossbow = mc.player.getMainHandStack().getItem() == Items.CROSSBOW;
                if (onIce || inSnow || holdingCrossbow) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (mc.player == null) return;
        if (!mode.is("Grim old") || !mc.player.isUsingItem()) return;

        Hand hand = mc.player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        mc.interactionManager.sendSequencedPacket(mc.world, seq ->
                new PlayerInteractItemC2SPacket(hand, seq, mc.player.getYaw(), mc.player.getPitch()));
    }
}
