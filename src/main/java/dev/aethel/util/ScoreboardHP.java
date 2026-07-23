package dev.aethel.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

public class ScoreboardHP {

    public static float getHealth(LivingEntity entity) {
        if (entity == null) return 0.0f;

        var mc = MinecraftClient.getInstance();

        if (entity instanceof ClientPlayerEntity) return entity.getHealth();

        if (!(entity instanceof PlayerEntity player) || mc.getCurrentServerEntry() == null) return entity.getHealth();

        try {
            Scoreboard scoreboard = player.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (objective == null) objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            if (objective == null) return entity.getHealth();

            var score = scoreboard.getScore(player, objective);
            if (score == null) return entity.getHealth();

            return score.getScore();
        } catch (Exception ignored) {
            return entity.getHealth();
        }
    }

    public static float getHealthWithAbsorption(LivingEntity entity) {
        return Math.max(0.0f, getHealth(entity) + entity.getAbsorptionAmount());
    }
}
