package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import dev.ethereal.api.event.orbit.EventHandler;
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

    public boolean shouldResolveScoreboardHealth() {
        return isRW() || isFT();
    }

    public float[] getHealthFromScoreboard(LivingEntity target) {
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();

        if (target == mc.player || !(target instanceof PlayerEntity player)) {
            return new float[]{health, maxHealth};
        }

        Float scoreboardHealth = readBelowNameHealth(player);
        if (scoreboardHealth != null) {
            health = scoreboardHealth;
            maxHealth = 20.0f;
        }

        return new float[]{health, maxHealth};
    }

    public Float readBelowNameHealth(PlayerEntity player) {
        ScoreboardObjective objective = player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (objective == null) return null;

        ReadableScoreboardScore score = player.getScoreboard().getScore(player, objective);
        MutableText text = ReadableScoreboardScore.getFormattedScore(score, objective.getNumberFormatOr(StyledNumberFormat.EMPTY));
        String parsed = text.getString().replaceAll("\\D", "");
        if (parsed.isEmpty() || parsed.equals("0")) return null;

        try {
            return Float.parseFloat(parsed);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!shouldResolveScoreboardHealth()) return;
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!PlayerUtil.isValidName(player.getName().getString())) continue;

            Float resolvedHealth = readBelowNameHealth(player);
            if (resolvedHealth != null) player.setHealth(resolvedHealth);
        }
    }
}
