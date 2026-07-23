package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;

@ModuleRegister(name = "NoServerPack", category = Category.OTHER)
public class NoServerPackModule extends Module {
    @Getter private static final NoServerPackModule instance = new NoServerPackModule();

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.isReceive() && event.packet() instanceof ResourcePackSendS2CPacket packet) {
            sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
            sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            event.setCancel(true);
        }
    }
}
