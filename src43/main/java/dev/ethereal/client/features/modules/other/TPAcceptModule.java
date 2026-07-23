package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.system.configs.FriendManager;

@ModuleRegister(name = "TP Accept", category = Category.OTHER)
public class TPAcceptModule extends Module {
    @Getter private static final TPAcceptModule instance = new TPAcceptModule();

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isReceive() && event.packet() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();

                if (message.contains("телепортироваться") || message.contains("tpaccept")) {
                    for (String name : FriendManager.getInstance().getData()) {
                        if (message.toLowerCase().contains(name.toLowerCase())) {
                            mc.player.networkHandler.sendChatCommand("tpaccept " + name);
                            break;
                        }
                    }
                }
            }
        }));

        addEvents(packetEvent);
    }
}
