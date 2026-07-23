package dev.ethereal.api.event.events.client;

import dev.ethereal.api.event.CancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.packet.Packet;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class PacketEvent extends CancellableEvent {
    private final Packet<?> packet;
    private final PacketType packetType;

    public boolean isReceive() {
        return packetType == PacketType.RECEIVE;
    }

    public boolean isSend() {
        return packetType == PacketType.SEND;
    }

    public enum PacketType {
        SEND, RECEIVE
    }
}
