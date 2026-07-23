package dev.ethereal.client.ui.widget;

import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.api.utils.render.fonts.Icons;
import java.awt.Color;

public abstract class InformationWidget extends Widget {
    public InformationWidget(float x, float y) {
        super(x, y);
    }

    @Override
    public void render(MatrixStack matrixStack) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        String valueText = getValue();
        Icons icon = getIcon();

        float padding = scaled(3f);
        float iconSize = scaled(8f);
        float fontSize = scaled(6f);
        float gap = scaled(2f);

        float valueWidth = getMediumFont().getWidth(valueText, fontSize);
        
        float backgroundWidth = padding + (icon != null ? iconSize + gap : 0) + valueWidth + padding;
        float backgroundHeight = padding + Math.max(iconSize, getMediumFont().getHeight(fontSize)) + padding;
        float round = scaled(4f);

        // Темный фон как на фото
        Color bgColor = new Color(17, 17, 20, 240);
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, bgColor);

        float currentX = x + padding;
        float centerY = y + backgroundHeight / 2f;

        // Иконка слева
        if (icon != null) {
            float iconY = centerY - iconSize / 2f;
            Fonts.ICONS.drawGradientText(matrixStack, icon.getLetter(), currentX, iconY, iconSize,
                UIColors.primary(), UIColors.secondary(), iconSize);
            currentX += iconSize + gap;
        }

        // Текст справа
        float textY = centerY - getMediumFont().getHeight(fontSize) / 2f;
        getMediumFont().drawText(matrixStack, valueText, currentX, textY, fontSize, Color.WHITE);

        getDraggable().setWidth(backgroundWidth);
        getDraggable().setHeight(backgroundHeight);
    }

    public abstract String getValue();
    public abstract Icons getIcon();
}