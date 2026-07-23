package sweetie.evaware.client.ui.widget.overlay;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import sweetie.evaware.api.system.configs.StaffManager;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.framelimiter.FrameLimiter;
import sweetie.evaware.api.utils.other.ReplaceUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;
import java.util.List;

public class StaffsWidget extends ContainerWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();
    private final Map<String, Float> animMap = new HashMap<>();

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
            animMap.put(k, animMap.getOrDefault(k, 0f) + (1f - animMap.getOrDefault(k, 0f)) * 0.15f);
        });
        animMap.entrySet().removeIf(e -> staffs.stream().noneMatch(s -> s.rawName().equals(e.getKey())) && e.getValue() < 0.05f);

        float x = getDraggable().getX(), y = getDraggable().getY();
        float dWidth = getDraggable().getWidth();
        boolean isRightSide = x + (dWidth / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float h = scaled(11), p = scaled(3.5f), fS = scaled(6f), iconS = scaled(8f);
        float arrowW = getMediumFont().getWidth(">", fS);
        Color bg = new Color(12, 12, 18, 240);
        Color graySeparator = new Color(160, 160, 160, 180);

        Map<String, Float> widthMap = new HashMap<>();
        float maxStaffW = 0;

        for (Staff s : staffs) {
            float nW = getMediumFont().getWidth(s.name(), fS);
            float lW = getMediumFont().getWidth(s.status().getLabel(), fS);
            float w = p + nW + p + arrowW + p + (lW + scaled(4)) + p;
            widthMap.put(s.rawName(), w);
            if (w > maxStaffW) maxStaffW = w;
        }

        String title = "Staffs";
        String iconChar = "A";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS.getWidth(iconChar, iconS);
        float headerMinW = titleW + iconW + p * 4;
        float headerW = Math.max(headerMinW, maxStaffW);

        staffs.sort((s1, s2) -> Float.compare(widthMap.getOrDefault(s2.rawName(), 0f), widthMap.getOrDefault(s1.rawName(), 0f)));

        float headerX = isRightSide ? (x + dWidth - headerW) : x;
        float currentY = y;

        RenderUtil.RECT.draw(ms, headerX, currentY, headerW, h, 3f, bg);
        getMediumFont().drawText(ms, title, headerX + p, currentY + h/2f - fS/2f, fS, Color.WHITE, 0f);
        Fonts.ICONS.drawGradientText(ms, iconChar, headerX + headerW - p - iconW, currentY + h/2f - iconS/2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        currentY += h + 1.5f;

        for (Staff s : staffs) {
            String k = s.rawName();
            float anim = animMap.getOrDefault(k, 0f);
            if (anim <= 0.05f) continue;

            float itemW = widthMap.getOrDefault(k, headerMinW);
            float itemX = isRightSide ? (x + dWidth - itemW) : x;

            float rowH = h * anim;
            int alpha = (int)(255 * anim);

            Color dynamicBg = new Color(0, 0, 0, (int)(205 * anim));
            Color dynamicText = new Color(255, 255, 255, alpha);
            Color dynamicArrow = new Color(graySeparator.getRed(), graySeparator.getGreen(), graySeparator.getBlue(), alpha);

            Color statusColorBase = switch (s.status()) {
                case ONLINE -> UIColors.positiveColor();
                case NEAR -> UIColors.middleColor();
                default -> UIColors.negativeColor();
            };
            Color dynamicStatusText = new Color(statusColorBase.getRed(), statusColorBase.getGreen(), statusColorBase.getBlue(), alpha);
            Color dynamicKeyBg = new Color(0, 0, 0, (int)(180 * anim));
            Color dynamicOutline = new Color(100, 100, 100, (int)(255 * anim));

            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, itemW, rowH, 3f, dynamicBg);

            float tY = currentY + (rowH / 2f) - fS/2f;

            getMediumFont().drawText(ms, s.name(), itemX + p, tY, fS, dynamicText, 0f);

            float nW = getMediumFont().getWidth(s.name(), fS);
            getMediumFont().drawText(ms, ">", itemX + p + nW + (p * 0.5f), tY, fS, dynamicArrow);

            String label = s.status().getLabel();
            float lW = getMediumFont().getWidth(label, fS);
            float kRectW = lW + scaled(4);
            float kRectH = (fS + scaled(2)) * anim;
            float kRectX = itemX + itemW - p - kRectW;
            float kRectY = currentY + (rowH / 2f) - (kRectH / 2f);

            if (anim > 0.5f) {
                RenderUtil.RECT.draw(ms, kRectX, kRectY, kRectW, kRectH, 2f, dynamicOutline);
                RenderUtil.RECT.draw(ms, kRectX + 0.5f, kRectY + 0.5f, kRectW - 1f, kRectH - 1f, 2f, dynamicKeyBg);
                getMediumFont().drawText(ms, label, kRectX + (kRectW / 2f) - (lW / 2f), tY, fS, dynamicStatusText, 0f);
            }

            currentY += rowH + 1.5f;
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
        String p = prefix.toLowerCase();
        return p.contains("helper") || p.contains("moder") || p.contains("admin") || p.contains("owner") ||
                p.contains("curator") || p.contains("куратор") || p.contains("модер") || p.contains("админ") ||
                p.contains("хелпер") || p.contains("стажер");
    }
}
