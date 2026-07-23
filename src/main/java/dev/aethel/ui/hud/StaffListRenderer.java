package dev.aethel.ui.hud;

import dev.aethel.module.list.render.Interface;
import dev.aethel.module.list.render.Interface.Status;
import dev.aethel.module.list.render.Interface.Staff;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.List;

public class StaffListRenderer implements IMinecraft {
    private final Interface interfaceModule;
    private final Animation alpha2 = new Animation(Easing.EXPO_OUT, 200);
    private final Animation widthAnim2 = new Animation(Easing.EXPO_OUT, 200);
    public StaffListRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void render(DrawContext context) {
        if (mc.player == null) return;

        renderCelestial(context);
    }

    private void renderCelestial(DrawContext context) {
        List<Staff> staffPlayers = interfaceModule.getStaffPlayers();
        final boolean chatOpen = mc.currentScreen instanceof ChatScreen;

        for (Staff staff : staffPlayers) {
            staff.animation.run(staff.isOnServer ? 1 : 0);
        }

        List<Staff> activeStaff = staffPlayers.stream()
                .filter(s -> s.animation.getValue() > 0.01f)
                .toList();

        alpha2.run((activeStaff.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha2.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);

        final String headerText = "Staff Online";

        final float fontSize = 7f;
        final float headerH = 13f;
        final float rowH = 11f;
        final float minRadius = 4f;

        float targetWidth = 70f;

        for (Staff staff : activeStaff) {
            float prefixW = Fonts.SFBOLD.get().getWidth(staff.prefix, fontSize);
            float nameW = Fonts.SFBOLD.get().getWidth(" " + staff.name, fontSize);

            float rowWidth = 5f + prefixW + nameW + 14f + 4f;
            targetWidth = Math.max(targetWidth, rowWidth);
        }

        widthAnim2.run(targetWidth);
        float curW = Math.max(70f, (float) widthAnim2.getValue());

        float rowsHeight = (float) activeStaff.stream()
                .mapToDouble(s -> rowH * MathHelper.clamp((float) s.animation.getValue(), 0f, 1f))
                .sum();

        float totalH = headerH + rowsHeight + (rowsHeight > 0f ? 3f : 1f);

        float x = interfaceModule.getStaffListDrag().getX();
        float y = interfaceModule.getStaffListDrag().getY();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        // Фон - приглушённый оттенок темы
        int bgColor = ColorProvider.rgba(
                ((t1 >> 16) & 0xFF) >> 2,
                ((t1 >> 8) & 0xFF) >> 2,
                (t1 & 0xFF) >> 2,
                Math.min(135, aInt)
        );

        // Глоу/обводка
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
        int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;

        Matrix4f m = context.getMatrices().peek().getPositionMatrix();

        interfaceModule.drawGlow(m, x, y, curW, totalH, minRadius, globalAlpha);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, curW + 1f, totalH + 1f, minRadius, glow[0], glow[1], glow[2], glow[3]);
        DrawUtil.drawRound(x, y, curW, totalH, minRadius, bgColor);

        // Иконка слева
        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "D", x + 4f, y + 2.5f, ColorProvider.rgba(255, 255, 255, aInt), 8f);
        DrawUtil.drawRound(x + 14f, y + 3f, 0.5f, headerH - 6f, 0.2f, ColorProvider.rgba(120, 120, 120, aInt / 2));

        // Текст заголовка по центру
        float headerTextX = x + (curW - Fonts.SFBOLD.get().getWidth(headerText, 7f)) / 2f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), headerText, headerTextX, y + 2.5f, ColorProvider.rgba(255, 255, 255, aInt), 7f);

        // Разделитель под заголовком
        int sepColor = ColorProvider.setAlpha(t1, aInt / 3);
        DrawUtil.drawRound(x + 3f, y + headerH, curW - 6f, 0.5f, 0.25f, sepColor);

        float curY = y + headerH + 1f;

        for (Staff staff : activeStaff) {
            float rowAnim = MathHelper.clamp((float) staff.animation.getValue(), 0f, 1f);
            if (rowAnim <= 0.001f) continue;

            float itemH = rowH * rowAnim;
            int itemA = MathHelper.clamp((int) (aInt * rowAnim), 0, 255);

            if (itemA >= 4) {
                float textY = curY + (itemH / 2f) - (fontSize / 2f) - 0.5f;

                // Префикс + имя стафа
                DrawUtil.drawText(Fonts.SFBOLD.get(), staff.prefix, x + 5f, textY, fontSize, itemA);

                float prefixW = Fonts.SFBOLD.get().getWidth(staff.prefix, fontSize);
                DrawUtil.drawText(Fonts.SFBOLD.get(), " " + staff.name, x + 5f + prefixW, textY,
                        ColorProvider.rgba(220, 220, 220, itemA), fontSize);

                // Статус-точка справа
                boolean inNear = mc.world != null && mc.world.getPlayers().stream()
                        .anyMatch(p -> p.getName().getString().equals(staff.name));

                int statusColor;
                if (staff.status == Status.VANISHED || staff.isSpec) statusColor = ColorProvider.rgba(255, 50, 50, itemA);
                else if (inNear) statusColor = ColorProvider.rgba(255, 215, 0, itemA);
                else statusColor = ColorProvider.rgba(50, 255, 50, itemA);

                float r = 2.5f;
                float dotX = x + curW - r * 2f - 4f;
                float dotY = curY + (itemH - r * 2f) / 2f;

                DrawUtil.drawRound(dotX, dotY, r * 2f, r * 2f, r, statusColor);
            }

            curY += itemH;
        }

        interfaceModule.getStaffListDrag().setWidth(curW);
        interfaceModule.getStaffListDrag().setHeight(totalH);
    }
}
