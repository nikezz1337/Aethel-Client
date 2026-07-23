package dev.ethereal.client.ui.widget.overlay;

import lombok.Getter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import dev.ethereal.api.system.configs.StaffManager;
import dev.ethereal.api.utils.framelimiter.FrameLimiter;
import dev.ethereal.api.utils.other.ReplaceUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.ui.widget.ListWidget;

import java.awt.Color;
import java.util.*;
import java.util.List;

public class StaffsWidget extends ListWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();

    public record Staff(String name, String rawName, Status status) {}

    @Getter
    public enum Status {
        ONLINE("Online", new Color(130, 255, 130)),
        NEAR("Near", new Color(255, 200, 95)),
        GM3("Gm3", new Color(255, 80, 80)),
        VANISH("Vanish", new Color(255, 80, 80));
        private final String label;
        private final Color color;
        Status(String label, Color color) { this.label = label; this.color = color; }
    }

    public StaffsWidget() { super(100f, 100f); }

    @Override public String getName() { return "Staffs"; }
    @Override protected String getIcon() { return "f"; }

    @Override
    protected List<Row> collectRows() {
        List<Row> rows = new ArrayList<>();
        for (Staff s : getStaffList()) {
            rows.add(new Row(s.rawName(), s.name(), s.status().getLabel(), s.status().getColor()));
        }
        return rows;
    }

    private List<Staff> getStaffList() {
        frameLimiter.execute(15, () -> {
            List<Staff> list = new ArrayList<>();
            if (mc.player != null && !mc.isInSingleplayer()) {
                list.addAll(getOnlineStaff());
                list.addAll(getVanishedPlayers());
            }
            cacheStaffs = list;
        });
        return cacheStaffs;
    }

    private List<Staff> getOnlineStaff() {
        List<Staff> staff = new ArrayList<>();
        if (mc.player == null || mc.player.networkHandler == null) return staff;
        for (PlayerListEntry player : mc.player.networkHandler.getPlayerList()) {
            String rawName = player.getProfile().getName();
            if (!PlayerUtil.isValidName(rawName)) continue;
            Team team = player.getScoreboardTeam();
            String prefix = team != null ? ReplaceUtil.replaceSymbols(team.getPrefix().getString()) : "";
            if (StaffManager.getInstance().contains(rawName) || isStaffPrefix(prefix.toLowerCase())) {
                Status status = Status.ONLINE;
                if (player.getGameMode() == GameMode.SPECTATOR) status = Status.GM3;
                else if (mc.world != null && mc.world.getPlayers().stream().anyMatch(p -> p.getGameProfile().getName().equals(rawName))) status = Status.NEAR;
                staff.add(new Staff(prefix + rawName, rawName, status));
            }
        }
        return staff;
    }

    private List<Staff> getVanishedPlayers() {
        List<Staff> vanished = new ArrayList<>();
        if (mc.world == null || mc.world.getScoreboard() == null || mc.getNetworkHandler() == null) return vanished;
        Set<String> onlineNames = new HashSet<>();
        mc.getNetworkHandler().getPlayerList().forEach(e -> onlineNames.add(e.getProfile().getName()));
        for (Team team : mc.world.getScoreboard().getTeams()) {
            for (String name : team.getPlayerList()) {
                if (PlayerUtil.isValidName(name) && !onlineNames.contains(name)) {
                    if (StaffManager.getInstance().contains(name)) vanished.add(new Staff(name, name, Status.VANISH));
                }
            }
        }
        return vanished;
    }

    private boolean isStaffPrefix(String prefix) {
        return prefix.contains("helper") || prefix.contains("moder") || prefix.contains("admin") || prefix.contains("owner") ||
                prefix.contains("curator") || prefix.contains("куратор") || prefix.contains("модер") || prefix.contains("админ") ||
                prefix.contains("хелпер") || prefix.contains("стажер");
    }
}
