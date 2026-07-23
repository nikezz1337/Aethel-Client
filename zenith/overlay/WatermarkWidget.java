package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.ui.widget.Widget;
import java.awt.Color;

public class WatermarkWidget extends Widget {
    private float animatedFps = 0;

    public WatermarkWidget() { super(4f, 4f); }
    @Override public String getName() { return "Watermark"; }

    @Override public void render(MatrixStack ms) {
        if (mc.player == null) return;

        final float h = scaled(14);
        final float p = scaled(4);
        final float fontSize = scaled(6);
        final float gap = scaled(2.5f);
        final float rectGap = 2.5f;

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        animatedFps = MathHelper.lerp(0.1f, animatedFps, mc.getCurrentFps());
        String fpsText = "FPS: " + Math.round(animatedFps);
        String ipText = (mc.getCurrentServerEntry() != null) ? mc.getCurrentServerEntry().address : "singleplayer";
        String pcName = System.getProperty("user.name");

        Color textC = Color.WHITE;
        Color bg = new Color(12, 12, 18, 240);

        // Название клиента
        String clientName = "quality";
        float wName = p + getMediumFont().getWidth(clientName, fontSize) + p;

        // Информация
        float wOther = p;
        wOther += getMediumFont().getWidth(pcName, fontSize) + gap;
        wOther += getMediumFont().getWidth(ipText, fontSize) + gap;
        wOther += getMediumFont().getWidth(fpsText, fontSize) + gap;
        wOther += p - gap;

        float currentX = x;
        RenderUtil.RECT.draw(ms, currentX, y, wName, h, 3, bg);

        float secondRectX = currentX + wName + rectGap;
        RenderUtil.RECT.draw(ms, secondRectX, y, wOther, h, 3, bg);

        float textY = y + (h / 2f) - (getMediumFont().getHeight(fontSize) / 2f) + 0.5f;

        // Рисуем название клиента с градиентом
        float nameX = currentX + p;
        getMediumFont().drawGradientText(ms, clientName, nameX, textY, fontSize, 
            UIColors.primary(), UIColors.secondary(), getMediumFont().getWidth(clientName, fontSize));

        // Рисуем информацию
        float otherX = secondRectX + p;
        getMediumFont().drawText(ms, pcName, otherX, textY, fontSize, textC, 0f);
        otherX += getMediumFont().getWidth(pcName, fontSize) + gap;

        getMediumFont().drawText(ms, ipText, otherX, textY, fontSize, textC, 0f);
        otherX += getMediumFont().getWidth(ipText, fontSize) + gap;

        getMediumFont().drawText(ms, fpsText, otherX, textY, fontSize, textC, 0f);

        getDraggable().setWidth(wName + rectGap + wOther);
        getDraggable().setHeight(h);
    }
}
