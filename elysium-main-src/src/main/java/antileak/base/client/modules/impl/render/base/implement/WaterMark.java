package antileak.base.client.modules.impl.render.base.implement;

import com.adl.nativeprotect.User;
import net.minecraft.client.util.math.MatrixStack;
import antileak.base.elysium;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.draggable.Draggable;
import antileak.base.api.utils.render.RenderUtils;
import antileak.base.api.utils.render.fonts.msdf.Fonts;
import antileak.base.client.modules.impl.render.base.InterfaceProcessing;

import java.awt.*;

public class WaterMark extends InterfaceProcessing {
    private boolean showFps = true;
    private boolean showMs = true;

    public static String getUsername() { return "zenZ"; }
    public static String getUID() { return "1"; }

    public WaterMark(Draggable draggable) { super(draggable); }

    public boolean isShowFps() { return showFps; }
    public void setShowFps(boolean v) { this.showFps = v; }
    public boolean isShowMs() { return showMs; }
    public void setShowMs(boolean v) { this.showMs = v; }
    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer myfont(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("myfont.ttf", size);
    }
    @Override
    public void onRender(EventRender.Default eventRender) {
        DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        String uid = User.getInstance().profile("uid");
        String username = User.getInstance().profile("username");
        String hwid = User.getInstance().profile("hwid");
        var matrices = eventRender.getContext().getMatrices();
        float x = draggable.getX(), y = draggable.getY();
        var logoFont = Fonts.getFont("logo", 30);
        var iconNew14 = Fonts.getFont("iconnew", 14);
        var iconNew15 = Fonts.getFont("iconnew", 15);
        var icon14 = Fonts.getFont("icon", 14);
        var suisse13 = Fonts.getFont("suisse", 13);

        float elysiumRectH = 16;
        String iconGlyph = "A";
        float iconW = logoFont.getStringWidth(iconGlyph);
        float iconX = x + (17 - iconW) / 2;
        float iconY = y + 5.5f;
        int iconTop;
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            iconTop = elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        } else {
            iconTop = ColorUtils.getThemeColor();
        }
        boolean drawSquares = isUnusualRectType();
        float rect2Pad = 3;
        int whiteColor = new Color(255, 255, 255, 255).getRGB();
        int grayColor = new Color(150, 150, 150, 255).getRGB();
        float textY = y + 6.8f;

        float brandTextX = iconX + iconW + 2.5f;
        float elysiumRectW = brandTextX + suisse13.getStringWidth("") + 1.5f - x;

        float rect2X = x + elysiumRectW + 2.5f;
        float rect2H = 15.85f;
        String iconGlyph2 = "e", iconGlyph3 = "f";
        float icon2Y = y + 7.45f, icon3Y = y + 7.25f;

        int fps = mc != null ? mc.getCurrentFps() : 0;
        String fpsValue = String.valueOf(fps), fpsSuffix = "fps", fpsText = fpsValue + fpsSuffix;

        int ping = 0;
        if (mc != null && mc.player != null && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        String pingValue = String.valueOf(ping), pingSuffix = "ms", pingText = pingValue + pingSuffix;

        String separator = ">";
        float sepW = suisse13.getStringWidth(separator);
        float sepGap = 4f;

        float drawX = rect2X + rect2Pad + 1.5f;
        float contentEndX = drawX;
        contentEndX += iconNew14.getStringWidth(iconGlyph2) + 1f;
        if (!username.isEmpty()) {
            contentEndX += suisse13.getStringWidth(username) + 2f;
        }
        if (showFps) {
            contentEndX += sepW + sepGap + myfont(14).getStringWidth(iconGlyph3) + 2f + suisse13.getStringWidth(fpsText) + 2f;
        }
        if (showMs) {
            contentEndX += sepW + sepGap + iconNew14.getStringWidth("m") + 2f + suisse13.getStringWidth(pingText) + 2f;
        }
        float combinedW = Math.max(contentEndX - x, 34.0f);

        RenderUtils.drawDefaultHudThemedPanel(matrices, x, y, combinedW, elysiumRectH, 3, 3, iconTop);
        if (drawSquares) RenderUtils.drawHudSquarePattern(matrices, x, y, combinedW, elysiumRectH, iconTop);

        RenderUtils.drawShadow(matrices, iconX + 6.6f, iconY - 1.25f, iconW - 8, 6, 3, 9f, ColorUtils.applyAlpha(iconTop, 0.32f));
        logoFont.drawGradientStringHorizontal(matrices, iconGlyph, iconX + 1f, iconY - 3, iconTop, iconTop);
        suisse13.drawString(matrices, separator, brandTextX + 1.5f, textY, grayColor);

        iconNew14.drawGradientStringHorizontal(matrices, iconGlyph2, drawX - 1f, icon2Y, iconTop, iconTop);
        drawX += iconNew14.getStringWidth(iconGlyph2) + 1f;
        if (!username.isEmpty()) { suisse13.drawString(matrices, username, drawX, textY, whiteColor); drawX += suisse13.getStringWidth(username) + 2f; }
        if (showFps) {
            suisse13.drawString(matrices, separator, drawX, textY, grayColor);
            drawX += sepW + sepGap;
            myfont(14).drawGradientStringHorizontal(iconGlyph3, drawX, icon3Y, iconTop, iconTop);
            drawX += myfont(14).getStringWidth(iconGlyph3) + 2f;
            suisse13.drawString(matrices, fpsValue, drawX, textY, whiteColor);
            suisse13.drawString(matrices, fpsSuffix, drawX + suisse13.getStringWidth(fpsValue) - 1, textY, iconTop);
            drawX += suisse13.getStringWidth(fpsText) + 2f;
        }
        if (showMs) {
            suisse13.drawString(matrices, separator, drawX, textY, grayColor);
            drawX += sepW + sepGap;
            iconNew14.drawGradientStringHorizontal(matrices, "m", drawX, icon3Y, iconTop, iconTop);
            drawX += iconNew14.getStringWidth("m") + 2f;
            suisse13.drawString(matrices, pingValue, drawX, textY, whiteColor);
            suisse13.drawString(matrices, pingSuffix, drawX + suisse13.getStringWidth(pingValue) - 0.5, textY, iconTop);
        }

        String serverName = "Singleplayer";
        if (mc != null) {
            var info = mc.getCurrentServerEntry();
            if (info != null && info.address != null && !info.address.isEmpty()) serverName = info.address;
        }

        float rectBtmY = y + elysiumRectH + 2f, rectBtmH = 15.85f;
        float iconSmallW = iconNew15.getStringWidth(iconGlyph);
        float iconSmallY = rectBtmY + (rectBtmH - 15) / 2f + 6.5f;
        float serverTextY = rectBtmY + (rectBtmH - 12f) / 2f + 4.8f;
        String serverDisplayName = formatServerNameForDisplay(serverName);
        float serverTextW = suisse13.getStringWidth(serverDisplayName);
        String extraIconGlyph = "y";
        float extraIconW = iconNew15.getStringWidth(extraIconGlyph);
        float extraIconY = rectBtmY + (rectBtmH - 15) / 2f + 6.4f;
        String tpsValue = formatOneDecimal(getServerTps()), tpsSuffix = "tps";
        float tpsTextW = suisse13.getStringWidth(tpsValue + tpsSuffix);
        draggable.setWidth(combinedW);
        draggable.setHeight(elysiumRectH);
    }

    private void drawServerNameWithThemeParts(MatrixStack matrices, String serverName, float x, float y, int themeColor, int whiteColor) {
        var font = Fonts.getFont("suisse", 13);
        String[] parts = serverName.split("\\.");
        if (parts.length < 2) { font.drawString(matrices, serverName, x, y, whiteColor); return; }
        String mainPart = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));
        font.drawString(matrices, mainPart, x, y, whiteColor);
        font.drawString(matrices, "." + parts[parts.length - 1], x + font.getStringWidth(mainPart) - 2f, y, themeColor);
    }

    private String formatServerNameForDisplay(String serverName) {
        if (serverName == null || serverName.isEmpty()) return "";
        String host = serverName;
        int portIndex = host.indexOf(':');
        if (portIndex > 0) host = host.substring(0, portIndex);
        String[] parts = host.split("\\.");
        return parts.length >= 3 ? String.join(".", java.util.Arrays.copyOfRange(parts, 1, parts.length)) : host;
    }

    private float getServerTps() {
        if (elysium.INSTANCE == null || elysium.INSTANCE.tpsCalc == null) return 20.0f;
        return Math.max(0.0f, Math.min(20.0f, elysium.INSTANCE.tpsCalc.getTPS()));
    }

    private String formatOneDecimal(float value) {
        int scaled = Math.round(value * 10.0f);
        return (scaled / 10) + "." + Math.abs(scaled % 10);
    }
}
