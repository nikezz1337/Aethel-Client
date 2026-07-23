package dev.aethel.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventPacket;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.util.regex.Pattern;

@ModuleInformation(
        moduleName = "PvP Safe",
        moduleCategory = ModuleCategory.MISC,
        moduleDesc = "Блокировка команд в PvP"
)
public class PvPSafe extends Module {

    private final BooleanSetting blockHub = new BooleanSetting("Блокировать хаб", true);
    private final BooleanSetting blockAnarchy = new BooleanSetting("Блокировать ан", false);

    private static final Pattern HUB_PATTERN = Pattern.compile("^(hub|lobby|leave|quit|exit)(\\s.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANARCHY_PATTERN = Pattern.compile("^an(archy)?(\\s*\\d+)(\\s.*)?$", Pattern.CASE_INSENSITIVE);

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.SEND) return;
        if (!(event.getPacket() instanceof CommandExecutionC2SPacket cmd)) return;

        String trimmed = cmd.command().trim();
        if (isBlocked(trimmed)) {
            event.cancelEvent();
        }
    }

    private boolean isBlocked(String command) {
        if (blockHub.getValue() && HUB_PATTERN.matcher(command).matches()) return true;
        if (blockAnarchy.getValue() && ANARCHY_PATTERN.matcher(command).matches()) return true;
        return false;
    }
}
