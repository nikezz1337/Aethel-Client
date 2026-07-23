package dev.ethereal.api.utils.color;

import lombok.experimental.UtilityClass;
import dev.ethereal.client.ui.theme.Theme;
import dev.ethereal.client.ui.theme.ThemeEditor;

import java.awt.*;

@UtilityClass
public class UIColors {
    public Theme currentTheme() {
        return ThemeEditor.getInstance().getCurrentTheme();
    }

    private Color getColor(Color color, int alpha) {
        int finalAlpha = (int) (color.getAlpha() / 255f * alpha);
        return ColorUtil.setAlpha(color, finalAlpha);
    }

    public Color gradient(int index) { return gradient(index, 255); }
    public Color gradient(int index, int alpha) { return getColor(ColorUtil.gradient(15, index, primary(alpha), secondary(alpha)), alpha); }

    public Color blur() { return blur(255); }
    public Color blur(int alpha) { return getColor(currentTheme().getBlurColor(), alpha); }

    public Color widgetBlur() { return widgetBlur(255); }
    public Color widgetBlur(int alpha) { return getColor(currentTheme().getWidgetBlurColor(), alpha); }

    public Color backgroundBlur() { return backgroundBlur(255); }
    public Color backgroundBlur(int alpha) { return getColor(currentTheme().getBackgroundBlurColor(), alpha); }

    public Color settingsBackground() { return settingsBackground(255); }
    public Color settingsBackground(int alpha) { 
        Color secondary = currentTheme().getSecondaryColor();
        Color darker = new Color(
            Math.max(0, secondary.getRed() - 155),
            Math.max(0, secondary.getGreen() - 155),
            Math.max(0, secondary.getBlue() - 155),
            secondary.getAlpha()
        );
        return getColor(darker, alpha);
    }

    public Color primary() { return primary(255); }
    public Color primary(int alpha) { return getColor(currentTheme().getPrimaryColor(), alpha); }

    public Color secondary() { return secondary(255); }
    public Color secondary(int alpha) { return getColor(currentTheme().getSecondaryColor(), alpha); }

    public Color textColor() { return textColor(255); }
    public Color textColor(int alpha) { return getColor(currentTheme().getTextColor(), alpha); }

    public Color inactiveTextColor() { return inactiveTextColor(255); }
    public Color inactiveTextColor(int alpha) { return getColor(currentTheme().getInactiveTextColor(), alpha); }
}