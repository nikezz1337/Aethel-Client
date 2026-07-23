package dev.ethereal.client.features.modules.other;

import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import lombok.Getter;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.system.configs.FriendManager;

@ModuleRegister(name = "Auto Accept", category = Category.OTHER)
public class TPAcceptModule extends Module {
    @Getter private static final TPAcceptModule instance = new TPAcceptModule();
    private final MultiBooleanSetting accept = new MultiBooleanSetting("Принимать").value(
            new BooleanSetting("Телепортацию").value(true),
            new BooleanSetting("Приглашение в клан").value(true)
    );

    public TPAcceptModule() {
        addSettings(accept);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.isReceive() && event.packet() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString();

            if (message.contains("приглашает Вас в клан") && accept.isEnabled("Приглашение в клан")) {
                for (String name : FriendManager.getInstance().getData()) {
                    if (message.toLowerCase().contains(name.toLowerCase())) {
                        mc.player.networkHandler.sendChatCommand("clan accept");
                        break;
                    }
                }
            }

            if (message.contains("телепортироваться") || message.contains("tpaccept")) {
                if (accept.isEnabled("Телепортацию")) {
                    for (String name : FriendManager.getInstance().getData()) {
                        if (message.toLowerCase().contains(name.toLowerCase())) {
                            mc.player.networkHandler.sendChatCommand("tpaccept " + name);
                            break;
                        }
                    }
                }
            }
        }
    }
}
