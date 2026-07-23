package dev.ethereal.api.utils.combat;

import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.system.interfaces.QuickImports;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

@Getter
public class ServerManager implements QuickImports {
    private static final ServerManager INSTANCE = new ServerManager();

    public static ServerManager getInstance() {
        return INSTANCE;
    }

    private int serverSlot;
    @Setter private float serverYaw, serverPitch;
    private float fallDistance;
    private double serverX, serverY, serverZ;
    private boolean serverOnGround, serverSprinting, serverSneaking, serverHorizontalCollision;

    private ServerManager() {
        Events.subscribe(this);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        double y = mc.player.prevY - mc.player.getY();
        if (mc.player.isOnGround()) fallDistance = 0;
        else if (y > 0) fallDistance += (float) y;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (e.isSend()) {
            if (e.packet() instanceof PlayerMoveC2SPacket packet) {
                if (packet.changesPosition()) {
                    serverX = packet.getX(mc.player.getX());
                    serverY = packet.getY(mc.player.getY());
                    serverZ = packet.getZ(mc.player.getZ());
                }

                if (packet.changesLook()) {
                    serverYaw = packet.getYaw(mc.player.getYaw());
                    serverPitch = packet.getPitch(mc.player.getPitch());
                }

                serverOnGround = packet.isOnGround();
                serverHorizontalCollision = packet.horizontalCollision();
            }

            if (e.packet() instanceof UpdateSelectedSlotC2SPacket packet) serverSlot = packet.getSelectedSlot();

            if (e.packet() instanceof ClientCommandC2SPacket packet) {
                switch (packet.getMode()) {
                    case START_SPRINTING -> serverSprinting = true;
                    case STOP_SPRINTING -> serverSprinting = false;
                    case PRESS_SHIFT_KEY -> serverSneaking = true;
                    case RELEASE_SHIFT_KEY -> serverSneaking = false;
                }
            }
        }
    }
}
