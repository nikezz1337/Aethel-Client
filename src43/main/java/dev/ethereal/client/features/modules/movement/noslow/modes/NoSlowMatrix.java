package dev.ethereal.client.features.modules.movement.noslow.modes;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import dev.ethereal.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowMatrix extends NoSlowMode {
    @Override
    public String getName() {
        return "Matrix";
    }

    private int useTime = 0;

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (!mc.player.isUsingItem()) {
            useTime = 0;
            return;
        }

        useTime = mc.player.getItemUseTime();

        if (useTime == 4) {
            sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                    BlockPos.ORIGIN,
                    mc.player.getHorizontalFacing()
            ));
        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean slowingCancel() {
        if (mc.player == null) return false;

        if (mc.player.isGliding()) return false;

        int currentUseTime = mc.player.getItemUseTime();

        return currentUseTime >= 4;
    }
}
