package dev.ethereal.client.ui.widget.overlay;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import dev.ethereal.api.system.configs.StaffManager;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.framelimiter.FrameLimiter;
import dev.ethereal.api.utils.other.ReplaceUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;
import java.util.List;

public class StaffsWidget extends ContainerWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();
    private final Map<String, Float> animMap = new HashMap<>();
    private final Map<String, Integer> orderMap = new HashMap<>();
    private int orderCounter = 0;

    public record Staff(String name, String rawName, Status status) {}

    @Getter
    public enum Status {
        ONLINE("Online"), NEAR("Near"), GM3("Gm3"), VANISH("Vanish");
        private final String label;
        Status(String label) { this.label = label; }
    }

    public StaffsWidget() { super(100f, 100f); }
    @Override public String getName() { return "Staffs"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public void render(MatrixStack ms) {
        List<Staff> staffs = new ArrayList<>(getStaffList());

        staffs.forEach(s -> {
            String k = s.rawName();
            if (!orderMap.containsKey(k)) orderMap.put(k, orderCounter++);
            float cur = animMap.getOrDefault(k, 0f);
            animMap.put(k, cur + (1f - cur) * 0.25f);
        });

        List<String> toRemove = new ArrayList<>();
        animMap.forEach((k, v) -> {
            if (staffs.stream().noneMatch(s -> s.rawName().equals(k))) {
                float nv = v - v * 0.2f;
                if (nv < 0.01f) toRemove.add(k);
                else animMap.put(k, nv);
            }
        });
        toRemove.forEach(k -> { animMap.remove(k); orderMap.remove(k); });

        float x = getDraggable().getX(), y = getDraggable().getY(), width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float headerH = scaled(12.7f), rowH0 = scaled(8.5f), p = scaled(2.5f), fS = scaled(5.5f), iconS = scaled(8f);
        float statusBoxW = scaled(22f);

        String title = "Staffs";
        String iconChar = "f";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS2.getWidth(iconChar, iconS);

        Map<String, Float> widthMap = new HashMap<>();
        float maxW = 0;
        for (Staff s : staffs) {
            float w = p + getMediumFont().getWidth(s.name(), fS) + p + scaled(1.5f) + statusBoxW + p;
            widthMap.put(s.rawName(), w);
            if (w > maxW) maxW = w;
        }

        float headerMinW = titleW + iconW + p * 4;
        float headerW = Math.max(headerMinW, maxW);
        float headerX = isRightSide ? (x + width - headerW) : x;
        float currentY = y;

        RenderUtil.BLUR_RECT.draw(ms, headerX, currentY, headerW, headerH, 2.5f, new Color(8, 8, 8, 200));
        getMediumFont().drawText(ms, title, headerX + p, currentY + headerH / 2f - fS / 2f, fS, Color.WHITE, 0f);
        Fonts.ICONS2.drawGradientText(ms, iconChar, headerX + headerW - p - iconW, currentY + headerH / 2f - iconS / 2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        currentY += headerH + scaled(1f);

        List<String> allKeys = new ArrayList<>();
        staffs.forEach(s -> { if (!allKeys.contains(s.rawName())) allKeys.add(s.rawName()); });
        animMap.keySet().forEach(k -> { if (!allKeys.contains(k)) allKeys.add(k); });
        allKeys.sort((k1, k2) -> Integer.compare(orderMap.getOrDefault(k1, 0), orderMap.getOrDefault(k2, 0)));

        for (String k : allKeys) {
            Float animValue = animMap.get(k);
            if (animValue == null || animValue <= 0.01f) continue;
            float anim = animValue;

            Staff s = staffs.stream().filter(st -> st.rawName().equals(k)).findFirst().orElse(null);
            float itemW = widthMap.getOrDefault(k, headerMinW);
            float xOffset = isRightSide ? (25f * (1f - anim)) : (-25f * (1f - anim));
            float itemX = (isRightSide ? (x + width - itemW) : x) + xOffset;

            float rowH = rowH0 * anim + 2.5f;
            int alpha = (int)(245 * anim);
            float gap = scaled(1.5f);
            float nameBlockW = itemW - gap - statusBoxW - p;
            Color blockColor = new Color(8, 8, 8, (int)(200 * anim));

            ms.push();

            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, nameBlockW, rowH, 2.5f, blockColor);
            if (s != null) {
                getMediumFont().drawText(ms, s.name(), itemX + p, currentY + rowH / 2f - fS / 2f, fS, new Color(255, 255, 255, alpha), 0f);
            }

            float kBoxX = itemX + nameBlockW + gap;
            RenderUtil.BLUR_RECT.draw(ms, kBoxX, currentY, statusBoxW, rowH, 2.5f, blockColor);

            if (s != null) {
                Color statusColor = switch (s.status()) {
                    case ONLINE -> new Color(130, 255, 130, alpha);
                    case NEAR -> new Color(255, 200, 95, alpha);
                    default -> new Color(255, 80, 80, alpha);
                };
                float lW = getMediumFont().getWidth(s.status().getLabel(), fS);
                getMediumFont().drawText(ms, s.status().getLabel(), kBoxX + statusBoxW / 2f - lW / 2f, currentY + rowH / 2f - fS / 2f, fS, statusColor, 0f);
            }

            ms.pop();
            currentY += rowH + scaled(1f);
        }

        getDraggable().setWidth(headerW);
        getDraggable().setHeight(currentY - y);
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
