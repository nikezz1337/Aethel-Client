package dev.aethel.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.aethel.config.StaffManager;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@ModuleInformation(
    moduleName = "AutoLeave",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Авто-выход при низком здоровье"
)
public class AutoLeave extends Module {

    private static final Set<String> STAFF_PREFIXES = new HashSet<>(Arrays.asList(
            "supp", "mod", "der", "adm", "wne", "curat", "dev", "yt",
            "мод", "помо", "адм", "владе", "курато", "сапп", "ютуб", "стажер", "сотрудник"
    ));

    public final SliderSetting leaveDistance = new SliderSetting("Дистанция срабатывания", 5, 3, 50, 1);
    public final BooleanSetting checkPlayer = new BooleanSetting("Игрок", true);
    public final BooleanSetting checkModerator = new BooleanSetting("Модератор", false);
    public final ModeSetting leaveType = new ModeSetting("Тип выхода", "В мейн меню", "В мейн меню", "/hub", "/home", "/spawn");
    public final BooleanSetting leaveDisable = new BooleanSetting("Выключать после выхода", true);

    private int cooldownTicks;

    @Override
    public void onEnable() {
        cooldownTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        cooldownTicks = 0;
        super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventPlayerUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        float maxDistance = (float) leaveDistance.getValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) continue;
            if (mc.player.distanceTo(player) <= maxDistance && shouldLeaveFor(player)) {
                triggerLeave();
                break;
            }
        }
    }

    private boolean shouldLeaveFor(PlayerEntity player) {
        if (isModerator(player)) return checkModerator.getValue();
        return checkPlayer.getValue();
    }

    private boolean isModerator(PlayerEntity player) {
        if (player == null) return false;
        String name = player.getName().getString();
        if (StaffManager.isStaff(name)) return true;
        var team = player.getScoreboardTeam();
        if (team == null) return false;
        String prefix = team.getPrefix().getString().toLowerCase(Locale.ROOT);
        for (String candidate : STAFF_PREFIXES) {
            if (prefix.contains(candidate)) return true;
        }
        return false;
    }

    private void triggerLeave() {
        switch (leaveType.getValue()) {
            case "В мейн меню" -> disconnectLeave();
            case "/hub" -> commandLeave("hub");
            case "/home" -> commandLeave("home home");
            case "/spawn" -> commandLeave("spawn");
        }
    }

    private void disconnectLeave() {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().getConnection().disconnect(Text.literal("AutoLeave"));
        if (leaveDisable.getValue()) toggle();
    }

    private void commandLeave(String command) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendChatCommand(command);
        cooldownTicks = leaveDisable.getValue() ? 10 : 30;
        if (leaveDisable.getValue()) toggle();
    }
}
