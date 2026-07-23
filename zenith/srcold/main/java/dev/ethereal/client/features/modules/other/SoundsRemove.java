package dev.ethereal.client.features.modules.other;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import lombok.Getter;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

@ModuleRegister(name = "SoundsRemove", category = Category.OTHER)
public class SoundsRemove extends Module {

    @Getter
    private static final SoundsRemove instance = new SoundsRemove();

    public SoundsRemove() {
    }

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::packetEvent));

        addEvents(packetEvent);
    }

    private void packetEvent(PacketEvent.PacketEventData event) {
        if (event.packet() instanceof PlaySoundS2CPacket soundPacket) {
            String soundPath = soundPacket.getSound().getIdAsString();

            if (soundPath.equals("minecraft:entity.player.levelup")
                    || soundPath.equals("minecraft:entity.experience_bottle.throw")
                    || soundPath.contains("experience_orb")) {
                PacketEvent.getInstance().setCancel(true);
            }
        }
    }
}
