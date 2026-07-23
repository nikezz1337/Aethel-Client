package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import ru.kotopushka.compiler.sdk.classes.Profile;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.core.Main;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends AbstractDraggable {
        private int fpsCount = 0;

        public Watermark() {
            super("Watermark", 10, 10, 92, 12, true);
        }

        @Override
        public void tick() {
            fpsCount = (int) MathUtil.interpolate(fpsCount, mc.getCurrentFps());
        }

        @Override
        public void drawDraggable(DrawContext e) {
            MatrixStack matrix = e.getMatrices();
            FontRenderer fontDef = Fonts.getSize(16, Fonts.Type.DEFAULT);
            FontRenderer fontIcons = Fonts.getSize(16, Fonts.Type.ICONS);

            float x = getX();
            float y = getY();
            float height = 12;

            String fps = "  " + mc.getCurrentFps();
            String role = "  " + Profile.getRole();
            LocalTime localTime = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String time = "  " + localTime.format(formatter);
            String coordsText = "  " + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();

            float otstup = 2f;
            float fpsWidth = fontDef.getStringWidth(fps);
            float roleWidth = fontDef.getStringWidth(role);
            float timeWidth = fontDef.getStringWidth(time);
            float coordsTextWidth = fontDef.getStringWidth(coordsText);
            float iconWidth = fontIcons.getStringWidth("U");
            float fpsIconWidth = fontIcons.getStringWidth("X");
            float roleIconWidth = fontIcons.getStringWidth("W");
            float timeIconWidth = fontIcons.getStringWidth("V");
            float coordsIconWidth = fontIcons.getStringWidth("S");

            blur.render(ShapeProperties.create(matrix, x - 2, y, iconWidth + 4, height).round(3).color(ColorUtil.getClientColor()).build());
            fontIcons.drawString(matrix, "U", x, y + 5, ColorUtil.getClientColor());
            float currentX = x + iconWidth + 4 + otstup;

            blur.render(ShapeProperties.create(matrix, currentX - 2, y, fpsWidth + fpsIconWidth + 4, height).round(3).color(ColorUtil.getClientColor()).build());
            fontIcons.drawString(matrix, "X", currentX + 1, y + 5, -1);
            fontDef.drawString(matrix, fps, currentX + fpsIconWidth, y + 4, -1);
            currentX += fpsWidth + fpsIconWidth + 4 + otstup;

            blur.render(ShapeProperties.create(matrix, currentX - 2, y, roleWidth + roleIconWidth + 4, height).round(3).color(ColorUtil.getClientColor()).build());
            fontIcons.drawString(matrix, "W", currentX + 1, y + 5, -1);
            fontDef.drawString(matrix, role, currentX + roleIconWidth, y + 4, -1);
            currentX += roleWidth + roleIconWidth + 4 + otstup;

            blur.render(ShapeProperties.create(matrix, currentX - 2, y, timeWidth + timeIconWidth + 4, height).round(3).color(ColorUtil.getClientColor()).build());
            fontIcons.drawString(matrix, "V", currentX + 1, y + 5, -1);
            fontDef.drawString(matrix, time, currentX + timeIconWidth, y + 4, -1);
            currentX += timeWidth + timeIconWidth + 4 + otstup;


            blur.render(ShapeProperties.create(matrix, currentX - 2, y, coordsTextWidth + coordsIconWidth + 4, height).round(3).color(ColorUtil.getClientColor()).build());
            fontIcons.drawString(matrix, "S", currentX + 1, y + 5, -1);
            fontDef.drawString(matrix, coordsText, currentX + coordsIconWidth, y + 4, -1);

            setWidth((int) (currentX + timeWidth + timeIconWidth + 4 - x));
        }
    }
