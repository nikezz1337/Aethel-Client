package antileak.base.client.modules.impl.combat;

import com.adl.nativeprotect.Native;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventPacket;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.client.modules.Module;

public class AntiThorns extends Module {

    public static final AntiThorns INSTANCE = new AntiThorns();

    private static final int ELYTRA_THORNS_VELOCITY_TICKS = 8;
    private static final byte THORNS_STATUS = 33;

    private int elytraThornsVelocityTicks = 0;
    private boolean thornsHit = false;

    public AntiThorns() {
        super("AntiThorns", "Отменяет урон от шипов", ModuleCategory.COMBAT);
    }
    @Native
    @EventLink
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) {
            elytraThornsVelocityTicks = 0;
            thornsHit = false;
            return;
        }

        if (event.getType() != EventPacket.Type.RECEIVE) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket packet
                && packet.getStatus() == THORNS_STATUS
                && packet.getEntity(mc.world) == mc.player) {
            thornsHit = true;
            if (mc.player.isGliding()) {
                elytraThornsVelocityTicks = ELYTRA_THORNS_VELOCITY_TICKS;
            }
            event.cancel();
            return;
        }

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet
                && packet.getEntityId() == mc.player.getId()) {
            if (thornsHit) {
                event.cancel();
                thornsHit = false;
                return;
            }
            if (shouldCancelElytraThornsVelocity()) {
                event.cancel();
                elytraThornsVelocityTicks = 0;
            }
        }
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) {
            elytraThornsVelocityTicks = 0;
            thornsHit = false;
            return;
        }

        if (!mc.player.isGliding()) {
            elytraThornsVelocityTicks = 0;
        }

        if (elytraThornsVelocityTicks > 0) {
            elytraThornsVelocityTicks--;
        }
    }

    @Override
    public void onEnable() {
        elytraThornsVelocityTicks = 0;
        thornsHit = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        elytraThornsVelocityTicks = 0;
        thornsHit = false;
        super.onDisable();
    }

    private boolean shouldCancelElytraThornsVelocity() {
        return elytraThornsVelocityTicks > 0 && mc.player.isGliding();
    }
}