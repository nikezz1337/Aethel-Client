package dev.ethereal.client.features.modules.other;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.other.NetworkUtil;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;

@ModuleRegister(name = "NoDesync", category = Category.OTHER)
public class NoDesync extends Module {
    @Getter
    private static final NoDesync instance = new NoDesync();

    public NoDesync() {}

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isReceive()) return;
        if (!(event.packet() instanceof UpdateSelectedSlotS2CPacket)) return;
        NetworkUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        event.setCancel(true);
    }
}
