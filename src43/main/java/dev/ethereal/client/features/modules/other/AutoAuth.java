package dev.ethereal.client.features.modules.other;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.math.TimerUtil;
import lombok.Getter;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

@ModuleRegister(name = "AutoAuth", category = Category.OTHER)
public class AutoAuth extends Module {
    @Getter private static final AutoAuth instance = new AutoAuth();

  //  private final StringSetting password = new StringSetting("Password").value("ohChort");
    private final TimerUtil timer = new TimerUtil();

    public AutoAuth() {
        addSettings(/*password*/);
    }

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive()) return;
            if (mc.player == null) return;
            if (!(event.packet() instanceof GameMessageS2CPacket packet)) return;

            String message = Formatting.strip(packet.content().getString());
            if (message == null) return;

            String pass = /*password.getValue()*/ "pasta";
            Map<String, String> commands = new HashMap<>();
            commands.put("/login", String.format("/login %s", pass));
            commands.put("/l", String.format("/l %s", pass));
            commands.put("/register", String.format("/register %s %s", pass, pass));
            commands.put("/reg", String.format("/reg %s %s", pass, pass));

            String[] split = message.split(" ");
            if (timer.finished(1000)) {
                StringBuilder builder = new StringBuilder();
                for (String msg : split) {
                    if (commands.containsKey(msg.toLowerCase())) {
                        builder.append(commands.get(msg.toLowerCase()));
                        timer.reset();
                        break;
                    }
                }
                if (!builder.isEmpty()) {
                    mc.player.networkHandler.sendChatMessage(builder.toString().trim());
                }
            }
        }));

        addEvents(packetEvent);
    }
}
