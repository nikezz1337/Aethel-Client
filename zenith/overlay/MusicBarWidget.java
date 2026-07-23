package sweetie.evaware.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.utils.media.MediaUtils;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Font;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MusicBarWidget extends Widget {
    private static final char WIFI_GLYPH = 'N';
    private Font iconFont;
    private float scrollOffset = 0f;
    private long lastUpdate = System.currentTimeMillis();

    public MusicBarWidget() {
        super(0f, 5f);
    }

    @Override
    public String getName() {
        return "MusicBar";
    }

    @Override
    public void render(MatrixStack matrixStack) {
        if (!isEnabled()) return;

        MediaUtils.MediaInfo mediaInfo = MediaUtils.getCurrentMedia();
        if (mediaInfo == null) return;

        String title = (mediaInfo.title == null || mediaInfo.title.isEmpty()) ? "Unknown" : mediaInfo.title;
        String artist = (mediaInfo.artist == null || mediaInfo.artist.isEmpty()) ? "Artist" : mediaInfo.artist;
        String label = title + " - " + artist;

        Font font = getMediumFont();
        Font boldFont = getSemiBoldFont();
        Color textColor = UIColors.textColor(255);
        Color accentColor = UIColors.primary(255);

        float fontSize = scaled(8.0f);
        float timeFontSize = scaled(9.0f);
        float artSize = scaled(14.0f);
        float panelHeight = scaled(18.0f);
        float maxTextWidth = scaled(100.0f);

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        float timeWidth = boldFont.getWidth(timeStr, timeFontSize);
        float timeGap = scaled(6.0f);
        float waveBlockWidth = scaled(12.5f);

        boolean hasArt = mediaInfo.getTexture() != null;
        float leftOffset = scaled(4.0f);
        float gapAfterArt = hasArt ? scaled(6.0f) : 0f;
        float rightOffset = scaled(6.0f);
        float gapAfterText = scaled(8.0f);

        float textWidth = font.getWidth(label, fontSize);
        float displayTabWidth = Math.min(textWidth, maxTextWidth);
        float panelWidth = leftOffset + (hasArt ? artSize + gapAfterArt : 0) + displayTabWidth + gapAfterText + waveBlockWidth + rightOffset;

        Font wifiFont = getIconFont();
        float wifiSize = scaled(10.0f);
        float wifiWidth = (wifiFont != null) ? wifiFont.getWidth(String.valueOf(WIFI_GLYPH), wifiSize) : 0.0f;
        float wifiGap = scaled(8.0f);

        float totalWidth = timeWidth + timeGap + panelWidth + (wifiWidth > 0 ? wifiGap + wifiWidth : 0);
        float groupX = (mc.getWindow().getScaledWidth() - totalWidth) / 2.0f;
        float y = getDraggable().getY();
        float centerY = y + (panelHeight / 2.0f);
        float barX = groupX + timeWidth + timeGap;

        boldFont.drawText(matrixStack, timeStr, groupX, centerY - (timeFontSize / 2.0f), timeFontSize, textColor);

        drawBackground(matrixStack, barX, y, panelWidth, panelHeight, scaled(9.0f));

        float currentX = barX + leftOffset;
        if (hasArt) {
            drawTexture(matrixStack, mediaInfo.getTexture(), currentX, centerY - (artSize / 2.0f), artSize, artSize, scaled(5.0f));
            currentX += artSize + gapAfterArt;
        }

        renderMarqueeText(matrixStack, font, label, currentX, centerY - (fontSize / 2.0f) + scaled(0.5f), displayTabWidth, fontSize, textColor);
        currentX += displayTabWidth + gapAfterText;

        renderVisualizer(matrixStack, mediaInfo, currentX, centerY, waveBlockWidth, accentColor, textColor);

        if (wifiFont != null) {
            float wifiX = barX + panelWidth + wifiGap;
            wifiFont.drawText(matrixStack, String.valueOf(WIFI_GLYPH), wifiX, centerY - (wifiSize / 2.0f), wifiSize, textColor);
        }

        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(panelHeight);
        getDraggable().setX(groupX);
    }

    private void renderMarqueeText(MatrixStack ms, Font font, String text, float x, float y, float maxWidth, float size, Color color) {
        float textWidth = font.getWidth(text, size);
        if (textWidth <= maxWidth) {
            font.drawText(ms, text, x, y, size, color);
            return;
        }

        long now = System.currentTimeMillis();
        float delta = (now - lastUpdate) / 1000f;
        lastUpdate = now;

        float speed = 20f;
        float gap = 30f;
        scrollOffset += delta * speed;
        if (scrollOffset > textWidth + gap) scrollOffset = 0;

        double scale = mc.getWindow().getScaleFactor();
        int scissorX = (int) (x * scale);
        int scissorY = (int) (mc.getWindow().getHeight() - (y + size + scaled(2)) * scale);
        int scissorW = (int) (maxWidth * scale);
        int scissorH = (int) ((size + scaled(5)) * scale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
        font.drawText(ms, text, x - scrollOffset, y, size, color);
        font.drawText(ms, text, x - scrollOffset + textWidth + gap, y, size, color);
        RenderSystem.disableScissor();
    }

    private void renderVisualizer(MatrixStack ms, MediaUtils.MediaInfo info, float x, float centerY, float width, Color accent, Color text) {
        if (info.status == MediaUtils.Status.PLAYING) {
            float barW = scaled(2.5f), barG = scaled(1.5f);
            float barBaseY = centerY + scaled(4.0f);
            for (int i = 0; i < 4; i++) {
                float h = Math.min(scaled(info.heights[i]), scaled(10.0f));
                RenderUtil.RECT.draw(ms, x + i * (barW + barG), barBaseY - h, barW, h, scaled(1.0f), accent);
            }
        } else {
            float pW = scaled(2.0f), pH = scaled(7.0f), pG = scaled(2.0f);
            float pX = x + (width - (pW * 2 + pG)) / 2.0f;
            RenderUtil.RECT.draw(ms, pX, centerY - (pH / 2.0f), pW, pH, scaled(0.5f), text);
            RenderUtil.RECT.draw(ms, pX + pW + pG, centerY - (pH / 2.0f), pW, pH, scaled(0.5f), text);
        }
    }

    private void drawBackground(MatrixStack ms, float x, float y, float w, float h, float r) {
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, r, UIColors.widgetBlur(255));
        RenderUtil.RECT.draw(ms, x, y, w, h, r, UIColors.backgroundBlur(255));
    }

    private void drawTexture(MatrixStack ms, AbstractTexture texture, float x, float y, float w, float h, float radius) {
        if (texture != null) {
            RenderUtil.TEXTURE_RECT.draw(ms, x, y, w, h, radius, Color.WHITE, 0f, 0f, 1f, 1f, texture.getGlId());
        }
    }

    private Font getIconFont() {
        if (iconFont != null) return iconFont;
        try {
            iconFont = Font.builder().find("other/icons").load();
        } catch (Exception ignored) {}
        return iconFont;
    }
}
