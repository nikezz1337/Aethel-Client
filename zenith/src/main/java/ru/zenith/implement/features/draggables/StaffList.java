package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.*;

public class StaffList extends AbstractDraggable {
    public static StaffList getInstance() {
        return Instance.getDraggable(StaffList.class);
    }

    public final List<StaffEntry> list = new ArrayList<>();
    private final List<String> staffPrefixes = List.of(
            "helper", "moder", "staff", "admin", "curator", "owner",
            "стажёр", "сотрудник", "помощник", "админ", "модер", "хелпер", "стажер"
    );

    public StaffList() {
        super("Staff List", 130, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.player.networkHandler == null) return;
        Collection<PlayerListEntry> playerList = mc.player.networkHandler.getPlayerList();

        for (PlayerListEntry entry : playerList) {
            if (entry.getProfile() == null || entry.getDisplayName() == null) continue;
            String rawName = entry.getProfile().getName();
            String displayStr = entry.getDisplayName().getString();
            String prefix = displayStr.replace(rawName, "");
            if (prefix.length() < 2) continue;

            boolean isStaff = staffPrefixes.stream().anyMatch(s -> prefix.toLowerCase().contains(s));
            if (!isStaff) continue;

            boolean alreadyTracked = list.stream().anyMatch(s -> s.rawName.equals(rawName));
            if (!alreadyTracked) {
                Status status = Status.ONLINE;
                if (entry.getGameMode() == GameMode.SPECTATOR) status = Status.GM3;
                else if (mc.world != null && mc.world.getPlayers().stream().anyMatch(p -> p.getGameProfile().getName().equals(rawName)))
                    status = Status.NEAR;

                StaffEntry se = new StaffEntry(rawName, prefix + rawName, status, new DecelerateAnimation().setMs(150).setValue(1));
                list.add(se);

                if (Hud.getInstance().notificationSettings.isSelected("Staff Join")) {
                    Notifications.getInstance().addList(displayStr + " - Зашел на сервер!", 5000);
                }
            }
        }

        // обновляем статусы и убираем ушедших
        Set<String> onlineNames = new HashSet<>();
        playerList.forEach(e -> { if (e.getProfile() != null) onlineNames.add(e.getProfile().getName()); });

        list.stream().filter(s -> !onlineNames.contains(s.rawName)).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
        list.removeIf(s -> s.anim.isFinished(Direction.BACKWARDS));
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font       = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontPlayer = Fonts.getSize(13, Fonts.Type.DEFAULT);

        float headerH = 17.5f;
        float rowH0   = 11f;
        float p       = 3f;
        float fS      = 13f;

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), headerH)
                .round(4, 0, 4, 0).softness(1).thickness(2)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9f)).build());

        blur.render(ShapeProperties.create(matrix, getX(), getY() + headerH, getWidth(), getHeight() - headerH)
                .round(0, 4, 0, 4).softness(1).thickness(2)
                .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7f)).build());

        float centerX = getX() + getWidth() / 2f;
        font.drawCenteredString(matrix, getName(), centerX, getY() + 7, ColorUtil.getText());

        int offset = (int) (headerH + 5);
        int maxWidth = 80;

        for (StaffEntry staff : list) {
            float anim = staff.anim.getOutput().floatValue();
            float centerY = getY() + offset;
            float width = fontPlayer.getStringWidth(staff.displayName) + 25;

            MathUtil.scale(matrix, centerX, centerY, 1, anim, () -> {
                // статус цвет
                int statusColor = switch (staff.status) {
                    case NEAR   -> ColorUtil.getColor(255, 200, 95);
                    case GM3    -> ColorUtil.getColor(255, 80, 80);
                    default     -> ColorUtil.getColor(130, 255, 130);
                };

                rectangle.render(ShapeProperties.create(matrix, getX() + 5, centerY - 1, 0.5f, 7)
                        .color(ColorUtil.getOutline(1, 0.5f)).build());
                fontPlayer.drawString(matrix, staff.displayName, getX() + 9, centerY + 1, ColorUtil.getText());

                // статус справа
                String label = staff.status.label;
                fontPlayer.drawString(matrix, label,
                        getX() + getWidth() - 5 - fontPlayer.getStringWidth(label),
                        centerY + 1, statusColor);
            });

            offset += (int) (rowH0 * anim);
            maxWidth = (int) Math.max(width, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }

    public enum Status {
        ONLINE("Online"), NEAR("Near"), GM3("Gm3"), VANISH("Vanish");
        public final String label;
        Status(String label) { this.label = label; }
    }

    private record StaffEntry(String rawName, String displayName, Status status, Animation anim) {}
}
