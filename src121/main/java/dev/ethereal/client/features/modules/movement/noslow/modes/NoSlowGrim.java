package dev.ethereal.client.features.modules.movement.noslow.modes;

import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import dev.ethereal.client.features.modules.movement.noslow.NoSlowMode;

public class NoSlowGrim extends NoSlowMode {
    @Override
    public String getName() {
        return "Grim";
    }

    public BypassType bypassType = BypassType.TICK;

    @Override
    public void onUpdate() {
        switch (bypassType) {
            case OLD -> {
                if (slowingCancel() && mc.player.isUsingItem()) {
                    Hand hand = mc.player.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    Rotation rotation = RotationManager.getInstance().getRotation();
                    // 1.21+ USE_ITEM packets are sequenced and carry rotation. Hardcoded
                    // sequence 0 or stale real rotations trip Grim BadPacketsH/J.
                    sendPacket(sequence -> new PlayerInteractItemC2SPacket(hand, sequence, rotation.getYaw(), rotation.getPitch()));
                }
            }

            case TICK -> {

            }
        }
    }

    @Override
    public void onTick() {
    }

    @Override
    public boolean slowingCancel() {
        boolean cancelRule = false;

        switch (bypassType) {
            case TICK -> {
                cancelRule = mc.player.getItemUseTime() % 2 == 0;
            }

            case OLD -> {
                cancelRule = true;
            }
        }

        return cancelRule;
    }

    public enum BypassType {
        TICK, OLD
    }
}
