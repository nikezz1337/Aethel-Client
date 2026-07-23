package dev.ethereal.api.utils.combat;

import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.system.interfaces.QuickImports;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

@Getter
public class ServerState implements QuickImports {
    @Getter private static final ServerState instance = new ServerState();

    private int serverSlot;
    private float serverYaw, serverPitch, fallDistance;
    private double serverX, serverY, serverZ;
    private boolean serverOnGround, serverSprinting, serverSneaking, serverHorizontalCollision;
    private boolean hasServerY;

    private ServerState() {
        Events.subscribe(this);
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (!event.isSend() || mc.player == null) return;

        if (event.packet() instanceof PlayerMoveC2SPacket packet) {
            // fallDistance зеркалит серверную логику handleFall: считается по нашим же
            // move-пакетам. Тик-сэмплы клиента пропускают приземления при быстром
            // движении, и счётчик навсегда застревает > 0.
            if (packet.changesPosition()) {
                double newY = packet.getY(mc.player.getY());
                if (packet.isOnGround()) fallDistance = 0;
                else if (hasServerY && newY < serverY) fallDistance += (float) (serverY - newY);
                hasServerY = true;

                serverX = packet.getX(mc.player.getX());
                serverY = newY;
                serverZ = packet.getZ(mc.player.getZ());
            } else if (packet.isOnGround()) {
                fallDistance = 0;
            }
            if (packet.changesLook()) {
                serverYaw = packet.getYaw(mc.player.getYaw());
                serverPitch = packet.getPitch(mc.player.getPitch());
            }
            serverOnGround = packet.isOnGround();
            serverHorizontalCollision = packet.horizontalCollision();
        }

        if (event.packet() instanceof UpdateSelectedSlotC2SPacket packet) {
            serverSlot = packet.getSelectedSlot();
        }

        if (event.packet() instanceof ClientCommandC2SPacket packet) {
            switch (packet.getMode()) {
                case START_SPRINTING -> serverSprinting = true;
                case STOP_SPRINTING -> serverSprinting = false;
                case PRESS_SHIFT_KEY -> serverSneaking = true;
                case RELEASE_SHIFT_KEY -> serverSneaking = false;
                default -> {}
            }
        }
    }
}
