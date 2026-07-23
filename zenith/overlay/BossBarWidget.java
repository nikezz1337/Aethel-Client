package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;
import java.util.Map;
import java.util.UUID;

public class BossBarWidget extends Widget {
    private float widgetWidth = 0f;
    private float widgetHeight = 0f;

    public BossBarWidget() {
        super(3f, 50f);
    }

    @Override
    public String getName() {
        return "BossBar";
    }

    @Override
    public void render(MatrixStack matrixStack) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float gap = getGap();

        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        BossBarHud bossBarHud = mc.inGameHud.getBossBarHud();
        if (bossBarHud == null) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        Map<UUID, ClientBossBar> bossBars = bossBarHud.bossBars;
        if (bossBars.isEmpty()) {
            getDraggable().setWidth(0f);
            getDraggable().setHeight(0f);
            return;
        }

        float currentY = y;
        float maxWidth = 0f;
        float totalHeight = 0f;

        for (ClientBossBar bossBar : bossBars.values()) {
            float[] barDimensions = renderBossBar(matrixStack, x, currentY, bossBar);
            maxWidth = Math.max(maxWidth, barDimensions[2]);
            totalHeight += barDimensions[3] + gap;
            currentY += barDimensions[3] + gap;
        }

        if (totalHeight > 0) {
            totalHeight -= gap;
        }

        widgetWidth = maxWidth;
        widgetHeight = totalHeight;

        getDraggable().setWidth(widgetWidth);
        getDraggable().setHeight(widgetHeight);
    }

    private float[] renderBossBar(MatrixStack matrixStack, float x, float y, ClientBossBar bossBar) {
        float fontSize = scaled(6f);
        float barHeight = scaled(6f);
        float barWidth = scaled(120f);
        float gap = getGap() * 0.8f;

        String name = bossBar.getName().getString();
        float progress = bossBar.getPercent();
        Color barColor = getBossBarColor(bossBar.getColor());
        Color backgroundColor = new Color(12, 12, 18, 220);

        float textWidth = getMediumFont().getWidth(name, fontSize);
        float contentWidth = Math.max(barWidth + gap * 2f, textWidth + gap * 3f);
        float backgroundHeight = fontSize + barHeight + gap * 2f;
        float round = backgroundHeight * 0.3f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, contentWidth, backgroundHeight, round, backgroundColor);

        float textX = x + (contentWidth - textWidth) / 2f;
        float textY = y + gap;
        getMediumFont().drawText(matrixStack, name, textX, textY, fontSize, Color.WHITE);

        float barX = x + (contentWidth - barWidth) / 2f;
        float barY = y + fontSize + gap * 1.5f;
        float barRound = barHeight * 0.2f;

        RenderUtil.BLUR_RECT.draw(matrixStack, barX, barY, barWidth, barHeight, barRound, new Color(20, 20, 25, 180));

        float progressWidth = barWidth * progress;
        if (progressWidth > 0) {
            RenderUtil.BLUR_RECT.draw(matrixStack, barX, barY, progressWidth, barHeight, barRound, barColor);
        }

        return new float[]{x, y, contentWidth, backgroundHeight};
    }

    private Color getBossBarColor(ClientBossBar.Color color) {
        switch (color) {
            case PINK:
                return new Color(255, 182, 193);
            case BLUE:
                return new Color(100, 149, 237);
            case RED:
                return new Color(255, 69, 69);
            case GREEN:
                return new Color(50, 205, 50);
            case YELLOW:
                return new Color(255, 215, 0);
            case PURPLE:
                return new Color(147, 112, 219);
            case WHITE:
                return new Color(255, 255, 255);
            default:
                return new Color(128, 128, 128);
        }
    }
}
