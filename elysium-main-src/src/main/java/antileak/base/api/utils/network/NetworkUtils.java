package antileak.base.api.utils.network;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.network.packet.Packet;
import antileak.base.api.QClient;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class NetworkUtils implements QClient {

    @Getter private final List<Packet<?>> silentPackets = new ArrayList<>();

    public void sendSilentPacket(Packet<?> packet) {
        silentPackets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    public void sendPacket(Packet<?> packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }
}