package dev.ethereal.client.features.modules.movement.noslow.modes;

import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.features.modules.movement.noslow.NoSlowMode;
import dev.ethereal.client.features.modules.movement.noslow.NoSlowModule;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;

public class NoSlowFunTime extends NoSlowMode {
    @Override
    public String getName() {
        return "FunTime";
    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean slowingCancel() {
        return PlayerUtil.getBlock(0, -1, 0) == Blocks.ICE && !(mc.player.getMainHandStack().getItem() == Items.TRIDENT) && NoSlowModule.getInstance().getFuntimeIce().getValue()
                || PlayerUtil.getBlock(0, 0, 0) == Blocks.SNOW && NoSlowModule.getInstance().getFuntimeSnow().getValue()
                || mc.player.getMainHandStack().getItem() == Items.CROSSBOW && NoSlowModule.getInstance().getFuntimeCrossBow().getValue();
    }
}
