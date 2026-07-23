package dev.aethel.ui.hud;

import dev.aethel.Aethel;
import dev.aethel.module.list.render.Interface;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import org.joml.Matrix4f;

public class WatermarkRenderer implements IMinecraft {
    private final Interface interfaceModule;

    public WatermarkRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void render(DrawContext context) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        renderCelestial(context);
    }

    private void renderCelestial(DrawContext context) {
        float x = interfaceModule.getWatermarkDrag().getX();
        float y = interfaceModule.getWatermarkDrag().getY();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();
        int aInt = 255;

        // Данные
        String fpsStr = mc.getCurrentFps() + " Fps";
        int pingMs = 0;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (entry != null) pingMs = entry.getLatency();
        String pingStr = pingMs + " ms";
        String tpsStr = String.format("%.1f Tps", Aethel.getInstance().getTpsGetter().getTPS());
        String coordsStr = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        double dX = mc.player.getX() - mc.player.prevX;
        double dZ = mc.player.getZ() - mc.player.prevZ;
        String bpsStr = String.format("%.1f Bps", Math.hypot(dX, dZ) * 20);

        float boxH = 15f;
        float gap = 2f;
        float radius = 4f;
        float iconSize = 8f;
        float textSize = 7f;
        float padL = 4f;
        float sg = 2f;

        int bgColor = ColorProvider.rgba(
                ((t1 >> 16) & 0xFF) >> 2,
                ((t1 >> 8) & 0xFF) >> 2,
                (t1 & 0xFF) >> 2,
                Math.min(135, aInt)
        );

        Matrix4f m = context.getMatrices().peek().getPositionMatrix();

        // Верхний ряд: FPS, Ping, TPS
        String[][] topData = {{"\u0058", fpsStr}, {"\u0051", pingStr}, {"\u0054", tpsStr}};
        float[] topW = new float[3];
        float topRowW = 0;
        for (int i = 0; i < 3; i++) {
            float iw = Fonts.ICONS_NURIK.get().getWidth(topData[i][0], iconSize);
            float tw = Fonts.SFBOLD.get().getWidth(topData[i][1], textSize);
            topW[i] = padL + iw + sg + 0.5f + sg + tw + padL;
            topRowW += topW[i];
            if (i < 2) topRowW += gap;
        }

        // Нижний ряд: Coords, BPS
        String[][] bottomData = {{"\u0046", coordsStr}, {"\u0040", bpsStr}};
        float[] bottomW = new float[2];
        float bottomRowW = 0;
        for (int i = 0; i < 2; i++) {
            float iw = Fonts.ICONS_NURIK.get().getWidth(bottomData[i][0], iconSize);
            float tw = Fonts.SFBOLD.get().getWidth(bottomData[i][1], textSize);
            bottomW[i] = padL + iw + sg + 0.5f + sg + tw + padL;
            bottomRowW += bottomW[i];
            if (i < 1) bottomRowW += gap;
        }

        float totalW = Math.max(topRowW, bottomRowW);
        float totalH = boxH + gap + boxH;
        int themeColor = ColorProvider.setAlpha(t1, 255);
        int sepColor = ColorProvider.rgba(180, 180, 185, 150);
        int textColor = ColorProvider.rgba(200, 200, 205, 255);
        float iconY = y + (boxH - iconSize) / 2f;
        float textY = y + (boxH - textSize) / 2f;

        // Рисуем верхний ряд
        float cx = x;
        for (int i = 0; i < 3; i++) {
            drawBox(m, cx, y, topW[i], boxH, radius, bgColor, t1, t2, aInt,
                    topData[i][0], iconSize, topData[i][1], textSize,
                    iconY, textY, themeColor, sepColor, textColor, padL, sg);
            cx += topW[i] + gap;
        }

        // Нижний ряд
        float row2Y = y + boxH + gap;
        float iconY2 = row2Y + (boxH - iconSize) / 2f;
        float textY2 = row2Y + (boxH - textSize) / 2f;
        cx = x;
        for (int i = 0; i < 2; i++) {
            drawBox(m, cx, row2Y, bottomW[i], boxH, radius, bgColor, t1, t2, aInt,
                    bottomData[i][0], iconSize, bottomData[i][1], textSize,
                    iconY2, textY2, themeColor, sepColor, textColor, padL, sg);
            cx += bottomW[i] + gap;
        }

        interfaceModule.getWatermarkDrag().setWidth(totalW);
        interfaceModule.getWatermarkDrag().setHeight(totalH);
    }

    private void drawBox(Matrix4f m, float x, float y, float w, float h, float radius,
                         int bgColor, int t1, int t2, int aInt,
                         String iconChar, float iconSize, String text, float textSize,
                         float iconY, float textY, int themeColor, int sepColor, int textColor,
                         float padL, float sg) {
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
        int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;

        interfaceModule.drawGlow(m, x, y, w, h, radius, 1.0f);
        DrawUtil.drawShadow(m, x, y, w, h, radius, 8f, ColorProvider.rgba(0, 0, 0, 80));
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, w + 1f, h + 1f, radius, glow[0], glow[1], glow[2], glow[3]);
        DrawUtil.drawRound(x, y, w, h, radius, bgColor);

        DrawUtil.drawText(Fonts.ICONS_NURIK.get(), iconChar, x + padL, iconY, themeColor, iconSize);

        float iconW = Fonts.ICONS_NURIK.get().getWidth(iconChar, iconSize);
        float sepX = x + padL + iconW + sg;
        DrawUtil.drawRound(sepX, y + 2.5f, 0.5f, h - 5f, 0.2f, sepColor);

        float textX = sepX + 0.5f + sg;
        DrawUtil.drawText(Fonts.SFBOLD.get(), text, textX, textY, textColor, textSize);
    }
}
