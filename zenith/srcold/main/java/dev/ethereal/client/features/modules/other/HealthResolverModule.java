package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.player.PlayerUtil;

@ModuleRegister(name = "FixHP", category = Category.OTHER)
public class HealthResolverModule extends Module {
    @Getter private static final HealthResolverModule instance = new HealthResolverModule();

    private final ModeSetting mode = new ModeSetting("Режим").value("FunTime").values("ReallyWorld", "FunTime");

    public HealthResolverModule() {
        addSettings(mode);
    }

    public boolean isRW() {
        return mode.is("ReallyWorld") && isEnabled();
    }

    public boolean isFT() {
        return mode.is("FunTime") && isEnabled();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isFT()) return;
            if (mc.getNetworkHandler() == null && mc.getNetworkHandler().getServerInfo() == null) return;

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (!PlayerUtil.isValidName(player.getName().getString())) continue;

                ScoreboardObjective scoreboard = null;
                String parsedHealth = "";
                if (player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                    scoreboard = player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                    if (scoreboard != null) {
                        ReadableScoreboardScore readableScoreboardScore = player.getScoreboard().getScore(player, scoreboard);
                        MutableText mutableText = ReadableScoreboardScore.getFormattedScore(readableScoreboardScore, scoreboard.getNumberFormatOr(StyledNumberFormat.EMPTY));
                        parsedHealth = mutableText.getString();
                    }
                }
                float resolvedHealth = 0f;
                try {
                    resolvedHealth = Float.parseFloat(parsedHealth);
                } catch (NumberFormatException ignored) {}

                if (!parsedHealth.isEmpty() && !parsedHealth.equals("0")) player.setHealth(resolvedHealth);
            }
        }));

        addEvents(tickEvent);
    }
}
