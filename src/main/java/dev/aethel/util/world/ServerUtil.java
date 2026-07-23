package dev.aethel.util.world;

import dev.aethel.Aethel;
import dev.aethel.module.list.player.FixHP;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.entity.PlayerIntersectionUtil;
import dev.aethel.util.other.StopWatch;

@Getter
@UtilityClass
public class ServerUtil implements IMinecraft {
    private final StopWatch pvpWatch = new StopWatch();
    public String server = "Vanilla";
    public float TPS = 20;
    public long timestamp;
    @Getter
    public int anarchy;
    @Getter
    public boolean pvpEnd;

    public void tick() {
        if (PlayerIntersectionUtil.nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) {
            server = "Vanilla";
            return;
        }
        anarchy = getAnarchyMode();
        server = getServer();
        pvpEnd = inPvpEnd();
        if (inPvp()) pvpWatch.reset();
    }

    public void updateTPS(long nanoTime) {
        float maxTPS = 20;
        float rawTPS = maxTPS * (1e9f / (nanoTime - timestamp));
        TPS = MathHelper.clamp(rawTPS, 0, maxTPS);
        timestamp = nanoTime;
    }

    private String getServer() {
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand().toLowerCase() : "";

        if (brand.contains("botfilter")) return "FunTime";
        else if (brand.contains("§6spooky§ccore")) return "SpookyTime";
        else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) return "CopyTime";
        else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        return "Vanilla";
    }

    private int getAnarchyMode() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        switch (server) {
            case "FunTime" -> {
                if (objective != null) {
                    String[] string = objective.getDisplayName().getString().split("-");
                    if (string.length > 1) {
                        try {
                            return Integer.parseInt(string[1]);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            case "HolyWorld" -> {
                for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objective)) {
                    String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                    if (!text.isEmpty()) {
                        String string = StringUtils.substringBetween(text, "#", " -◆-");
                        if (string != null && !string.isEmpty()) {
                            try {
                                return Integer.parseInt(string.replace(" (1.20)", ""));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return -1;
    }

    public boolean isPvp() {
        return !pvpWatch.finished(500);
    }

    private boolean inPvp() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
    }

    private boolean inPvpEnd() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase())
                .anyMatch(s -> (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1")));
    }

    static float getEntityHealth(Entity target) {
        if (target == null || mc.world == null || !(target instanceof LivingEntity e)) {
            return mc.player != null ? mc.player.getHealth() : 0;
        }

        if (!Aethel.getInstance().getModuleStorage().get(FixHP.class).isEnabled()) {
            return ((LivingEntity) target).getHealth();
        }

        if (!(target instanceof PlayerEntity)) {
            return e.getHealth();
        }

        if (target == mc.player) return mc.player.getHealth();
        if (target.isInvisible() && FixHP.mode.is("FunTime")) return 1000.0f;

        Scoreboard scoreboard = mc.world.getScoreboard();

        if (scoreboard != null && (FixHP.mode.is("FunTime") || FixHP.mode.is("ReallyWorld"))) {
            ScoreHolder scoreHolder = ScoreHolder.fromName(target.getName().getString());
            for (ScoreboardObjective objective : scoreboard.getObjectives()) {
                ReadableScoreboardScore score = scoreboard.getScore(scoreHolder, objective);
                if (score != null) {
                    return score.getScore();
                }
            }
        } else {
            return e.getHealth();
        }

        return 1000;
    }

    public static float getHealthFloat(LivingEntity target) {
        float health = getEntityHealth(target);
        if (!(target instanceof PlayerEntity)) {
            return health;
        }

        if (!Aethel.getInstance().getModuleStorage().get(FixHP.class).isEnabled()) return target.getHealth();

        return health == 1000 ? 20 : health;
    }

    public static String getHealthString(LivingEntity target) {
        float health = getEntityHealth(target);
        if (!(target instanceof PlayerEntity)) {
            return Math.round(health) + "";
        }

        return health == 1000 ? "?" : Math.round(health) + "";
    }


    public String getWorldType() {
        return mc.world.getRegistryKey().getValue().getPath();
    }

    public boolean isCopyTime() {
        return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");
    }

    public boolean isFunTime() {
        return server.equals("funtime");
    }

    public boolean isReallyWorld() {
        return server.equals("ReallyWorld");
    }

    public boolean isHolyWorld() {
        return server.equals("HolyWorld");
    }

    public boolean isSpookyTime() {
        return server.equals("SpookyTime");
    }

    public boolean isVanilla() {
        return server.equals("Vanilla");
    }
}
