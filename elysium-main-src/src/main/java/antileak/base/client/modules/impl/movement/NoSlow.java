package antileak.base.client.modules.impl.movement;

import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventSlowWalking;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.player.ViaProtocolUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.ModeSetting;
@SuppressWarnings("all")
public class NoSlow extends Module {

    public static NoSlow INSTANCE = new NoSlow();

    private final ModeSetting mode = new ModeSetting("Мод", "Grim Old", "Grim Old", "Grim Last");
    private final BooleanSetting sprint = new BooleanSetting("Спринт", true);

    public NoSlow() {
        super("NoSlow", "Убирает замедление во время еды", ModuleCategory.MOVEMENT);
        addSettings(mode, sprint);
    }

    @EventLink
    public void onSlowDown(EventSlowWalking event) {
        if (mc.player == null || !mc.player.isUsingItem()) return;

        if (mode.is("Grim Last")) {
            if (mc.player.getItemUseTime() % 2 == 0) {
                event.setCancelled(true);
            }
        }

        if (mode.is("Grim Old")) {
            Hand activeHand = mc.player.getActiveHand();
            boolean legacyProtocol = ViaProtocolUtils.isTargetProtocolBelowOneNineteen();

            if (sprint.isState()) {
                mc.player.setSprinting(
                        ((ModuleClass.INSTANCE.sprint.isEnable() && Sprint.isSprinting()) || mc.options.sprintKey.isPressed())
                                && mc.player.input.movementForward > 0
                                && (!legacyProtocol || (!mc.player.horizontalCollision && !mc.player.collidedSoftly))
                                && !mc.player.isGliding()
                );
            }

            Hand otherHand = activeHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(otherHand, 0, mc.player.getYaw(), mc.player.getPitch()));

            event.setCancelled(true);
        }
    }
}
