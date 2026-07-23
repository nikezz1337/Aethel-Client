package antileak.base.client.modules.impl.movement;

import com.adl.nativeprotect.Native;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.client.modules.Module;

public class GrimNoFall extends Module {

    public static GrimNoFall INSTANCE = new GrimNoFall();

    public GrimNoFall() {
        super("NoFall", "Убирает урон от падения", ModuleCategory.MOVEMENT);
    }
    @Native
    @EventLink
    public void onUpdate(final EventUpdate ignored) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!mc.player.isOnGround() && mc.player.fallDistance > 1f) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true, false));
            mc.player.onLanding();
        }
    }

}